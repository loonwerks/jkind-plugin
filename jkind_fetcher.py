#!/usr/local/bin/python2.7
# encoding: utf-8
'''Detect released versions of JKind and package as Eclipse plugin

This module uses the Github API v3 to query the released versions of
the JKind model checker.  For the detected releases the binaries are
packaged as an Eclipse plugin.  Optionally, a list of versions in a
file are skipped.  Also, versions already packaged are skipped as
well.

@copyright:  2019 Collins Aerospace. All rights reserved.

@license:    BSD 3-Clause License

'''

from __future__ import print_function

import contextlib
import os
import re
import shutil
import subprocess
import sys
import tempfile
import time

from argparse import ArgumentParser
from argparse import RawDescriptionHelpFormatter
from git import Repo
from github3 import GitHub
from mako.template import Template
from pprint import pprint, pformat
from shutil import copyfile
from zipfile import ZipFile



__all__ = []
__version__ = 0.1
__date__ = '2019-03-29'
__updated__ = '2019-03-29'

AUTH_TOKEN = os.environ['GH_TOKEN'] if 'GH_TOKEN' in os.environ.keys() else None

BASE_PACKAGE = 'com.collins.trustedsystems.jkindapi'
UPDATES_PACKAGE = '.'.join([BASE_PACKAGE, 'updates'])
TARGET_PACKAGE = '.'.join([BASE_PACKAGE, 'target'])

DEBUG = 1

TEMPLATE_DIR = 'plugin-templates'
PLUGIN_DIR = 'plugin'

GITHUB_URL = 'https://github.com'
GITHUB_API = 'https://api.github.com/repos'
GITHUB_RELEASES = 'releases'

JKIND_CHECKER_OWNER = 'agacek'
JKIND_CHECKER_REPO = 'jkind'
JKIND_CHECKER_REQUEST = '/'.join([GITHUB_API, JKIND_CHECKER_OWNER, JKIND_CHECKER_REPO, GITHUB_RELEASES])

JKIND_PLUGIN_OWNER = 'loonwerks'
JKIND_PLUGIN_REPO = 'jkind-plugin'
JKIND_PLUGIN_REQUEST = '/'.join([GITHUB_API, JKIND_PLUGIN_OWNER, JKIND_PLUGIN_REPO, GITHUB_RELEASES])


class CLIError(Exception):
    '''Generic exception to raise and log different fatal errors.'''
    def __init__(self, msg):
        super(CLIError).__init__(type(self))
        self.msg = "E: %s" % msg
    def __str__(self):
        return self.msg
    def __unicode__(self):
        return self.msg


@contextlib.contextmanager
def pushd(new_dir):
	old_dir = os.getcwd()
	os.chdir(new_dir)
	try:
		yield
	finally:
		os.chdir(old_dir)


def list_files(dir):
    r = []
    for root, dirs, files in os.walk(dir):
        for name in files:
            r.append(os.path.join(root, name))
    return r


def get_immediate_subdirectories(a_dir):
    return [name for name in os.listdir(a_dir)
            if os.path.isdir(os.path.join(a_dir, name))]


def copy_tree(src_dir, dst_dir):
    # Need our own copy_tree as shutil.copytree() requires that the destination
    # directory not exist prior to being called.
    for src_file in list_files(src_dir):
        dest_file = os.path.join(dst_dir, os.path.relpath(src_file, src_dir))
        dest_file_dir = os.path.dirname(dest_file)
        if not os.path.exists(dest_file_dir):
            os.makedirs(dest_file_dir)
        shutil.copy(src_file, dest_file)
        print("Copied %s to %s" % (src_file, dest_file))


def populate_plugin(jkind_tag, jkind_version, bundle_version, is_snapshot_version):
    gitrepo = Repo(os.getcwd())

    gitrepo.git.rm('-r', '-f', '--ignore-unmatch', PLUGIN_DIR)
    if os.path.exists(PLUGIN_DIR):
        shutil.rmtree(PLUGIN_DIR)

    for template_file in list_files(TEMPLATE_DIR):
        plugin_file = os.path.join(PLUGIN_DIR, os.path.relpath(template_file, TEMPLATE_DIR))
        template = Template(filename=template_file)
        if not os.path.exists(os.path.dirname(plugin_file)):
            os.makedirs(os.path.dirname(plugin_file))
        with open(plugin_file, 'w') as text_file:
            text_file.write(template.render(jkind_version=jkind_version, bundle_version=bundle_version))
        print("Wrote %s" % (plugin_file))

    jkind_dir = "jkind"
    jkind_common_dir = os.path.join(jkind_dir, "jkind-common")
    jkind_common_src_dir = os.path.join(jkind_common_dir, "src")
    jkind_api_dir = os.path.join(jkind_dir, "jkind-api")
    jkind_api_src_dir = os.path.join(jkind_api_dir, "src")
    jkind_api_icon_dir = os.path.join(os.path.join(jkind_api_dir, "resources"), "icons")

    api_bundle_dir = os.path.join(PLUGIN_DIR, "com.collins.trustedsystems.jkindapi")
    api_bundle_src_dir = os.path.join(api_bundle_dir, "src")
    api_bundle_icon_dir = os.path.join(api_bundle_dir, "icons")
    copy_tree(jkind_common_src_dir, api_bundle_src_dir)
    copy_tree(jkind_api_src_dir, api_bundle_src_dir)
    copy_tree(jkind_api_icon_dir, api_bundle_icon_dir)

    api_bundle_dependency_dir = os.path.join(api_bundle_dir, "dependencies")
    jkind_jar_file = os.path.join(jkind_dir, "jkind", "build", "libs", "jkind.jar")
    if not os.path.exists(api_bundle_dependency_dir):
        os.makedirs(api_bundle_dependency_dir)
    shutil.copy(jkind_jar_file, api_bundle_dependency_dir)
    print("Copied %s to directory %s" % (jkind_jar_file, api_bundle_dependency_dir))

    gitrepo.git.add(PLUGIN_DIR)


def update_composite_repository(bundle_version):
    gitrepo = Repo(os.getcwd())
    COMPOSITE_ARTIFACTS_TEMPLATE = Template("""<?xml version='1.0' encoding='UTF-8'?>
<?compositeArtifactRepository version='1.0.0'?>
<repository name='&quot;Eclipse Project Test Site&quot;'
    type='org.eclipse.equinox.internal.p2.artifact.repository.CompositeArtifactRepository' version='1.0.0'>
  <properties size='1'>
    <property name='p2.timestamp' value='${timestamp}'/>
  </properties>
  <children size='${len(children)}'>
% for child in children:
    <child location='${child}'/>
% endfor
  </children>
</repository>
""")
    COMPOSITE_CONTENT_TEMPLATE = Template("""<?xml version='1.0' encoding='UTF-8'?>
<?compositeMetadataRepository version='1.0.0'?>
<repository name='&quot;Eclipse Project Test Site&quot;'
    type='org.eclipse.equinox.internal.p2.metadata.repository.CompositeMetadataRepository' version='1.0.0'>
  <properties size='1'>
    <property name='p2.timestamp' value='${timestamp}'/>
  </properties>
  <children size='${len(children)}'>
% for child in children:
    <child location='${child}'/>
% endfor
  </children>
</repository>
""")
    composite_repo_dir = 'compositeRepository'
    composite_artifacts_file = os.path.join(composite_repo_dir, 'compositeArtifacts.xml')
    composite_content_file = os.path.join(composite_repo_dir, 'compositeContent.xml')
    if not os.path.exists(composite_repo_dir):
        os.makedirs(composite_repo_dir)
    timestamp = str(int(time.time() * 1000.0))
    copy_tree(os.path.join(PLUGIN_DIR, UPDATES_PACKAGE, 'target', 'repository'), os.path.join(composite_repo_dir, '%s_%s' % (BASE_PACKAGE, bundle_version)))
    children = get_immediate_subdirectories(composite_repo_dir)
    with open(composite_artifacts_file, 'w') as text_file:
        text_file.write(COMPOSITE_ARTIFACTS_TEMPLATE.render(children=children, timestamp=timestamp))
    print("Wrote %s" % (composite_artifacts_file))
    with open(composite_content_file, 'w') as text_file:
        text_file.write(COMPOSITE_CONTENT_TEMPLATE.render(children=children, timestamp=timestamp))
    print("Wrote %s" % (composite_content_file))    
    gitrepo.git.add(composite_repo_dir)


def commit_and_push_repo(bundle_version):
    gitrepo = Repo(os.getcwd())

    print('calling git commit...')
    git_result = gitrepo.git.commit('-m', 'JKind plug-in version %s' % (bundle_version), with_extended_output=True)
    print(git_result[1])
    if (git_result[0] != 0):
        print(git_result[2], file=sys.stderr)
        sys.exit(git_result[0])

    print('calling git tag...')
    git_result = gitrepo.git.tag(jkind_tag, with_extended_output=True)
    print(git_result[1])
    if (git_result[0] != 0):
        print(git_result[2], file=sys.stderr)
        sys.exit(git_result[0])

    print('calling git push...')
    git_result = gitrepo.git.push('--quiet', '--set-upstream', 'origin-with-token', 'master', with_extended_output=True)
    print(git_result[1])
    if (git_result[0] != 0):
        print(git_result[2], file=sys.stderr)
        sys.exit(git_result[0])

    print('calling git push tags...')
    git_result = gitrepo.git.push('origin-with-token', '--tags', with_extended_output=True)
    print(git_result[1])
    if (git_result[0] != 0):
        print(git_result[2], file=sys.stderr)
        sys.exit(git_result[0])
    print('git update and push complete.')


def release_plugin(bundle_version):
    gh = GitHub(GITHUB_API, token=AUTH_TOKEN)
    repository = gh.repository(JKIND_PLUGIN_OWNER, JKIND_PLUGIN_REPO)
    release = repository.create_release(bundle_version,
        target_commitish='master',
        name='JKIND Plugin %s' % (bundle_version),
        body='Eclipse plugin containing binaries for JKIND version %s.' % (bundle_version),
        draft=False,
        prerelease=False,
    )
    filename = '%s-%s.zip' % (UPDATES_PACKAGE, bundle_version)
    filepath = os.path.join(UPDATES_PACKAGE, 'target', filename)
    asset = release.upload_asset(content_type='application/binary', name=filename, asset=open(filepath, 'rb'))
    print('Uploaded release %s' % (filepath))


def package_plugin(jkind_tag, jkind_version, bundle_version, is_snapshot_version, release_description):
    '''Package a plugin from the exectuables for the corresponding release'''
    if release_description:
        print('Building plugin version %s for JKIND version %s...' % (bundle_version, jkind_version))

        
        # Fetch a clone of JKind
        if os.path.exists('jkind'):
            shutil.rmtree('jkind')
        jkind_url = '/'.join([GITHUB_URL, JKIND_CHECKER_OWNER, JKIND_CHECKER_REPO + '.git'])
        clone_result = subprocess.run(['git', 'clone', '-b', jkind_tag, jkind_url, 'jkind'], stderr=subprocess.STDOUT)
        print(clone_result.stdout, file=sys.stderr)
        if (clone_result.returncode != 0):
            sys.exit(clone_result.returncode)

        # Apply Gradle to build the JKind jars
        with pushd('jkind'):
            gradle_result = subprocess.run(['./gradlew', 'clean', 'jar'], stderr=subprocess.STDOUT)
            print(gradle_result.stdout, file=sys.stderr)
            if (gradle_result.returncode != 0):
                sys.exit(gradle_result.returncode)

        # Populate the plugin with sources and jars
        populate_plugin(jkind_tag, jkind_version, bundle_version, is_snapshot_version)

        # Apply Maven to build the plugin, feature and update site
        with pushd('plugin'):
            mvn_result = subprocess.run(['mvn', 'clean', 'verify'], stderr=subprocess.STDOUT)
            print(mvn_result.stdout, file=sys.stderr)
            if (mvn_result.returncode != 0):
                sys.exit(mvn_result.returncode)

        update_composite_repository(bundle_version)
        #if not is_snapshot_version:
        #    commit_and_push_repo()
        #release_plugin(bundle_version)

    else:
        print('Cannot find release description for %s' % (jkind_version), file=sys.stderr)


def main(argv=None): # IGNORE:C0111
    '''Command line options.'''

    if argv is None:
        argv = sys.argv
    else:
        sys.argv.extend(argv)

    program_name = os.path.basename(sys.argv[0])
    program_version = "v%s" % __version__
    program_build_date = str(__updated__)
    program_version_message = '%%(prog)s %s (%s)' % (program_version, program_build_date)
    program_shortdesc = __import__('__main__').__doc__.split("\n")[1]
    program_license = '''%s
Copyright 2019 Collins Aerospace. All rights reserved.

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions are met:
    * Redistributions of source code must retain the above copyright
      notice, this list of conditions and the following disclaimer.
    * Redistributions in binary form must reproduce the above copyright
      notice, this list of conditions and the following disclaimer in the
      documentation and/or other materials provided with the distribution.
    * Neither the name of Collins Aerospace nor the
      names of its contributors may be used to endorse or promote products
      derived from this software without specific prior written permission.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
DISCLAIMED. IN NO EVENT SHALL COLLINS AEROSPACE BE LIABLE FOR ANY
DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
(INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
(INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
''' % (program_shortdesc)

    try:
        # Setup argument parser
        parser = ArgumentParser(description=program_license, formatter_class=RawDescriptionHelpFormatter)
        parser.add_argument("-v", "--verbose", dest="verbose", action="count", help="set verbosity level [default: %(default)s]")
        parser.add_argument("-i", "--include", dest="include", help="only include releasess matching this regex pattern. Note: exclude is given preference over include. [default: %(default)s]", metavar="RE" )
        parser.add_argument("-e", "--exclude", dest="exclude", help="exclude paths matching this regex pattern. [default: %(default)s]", metavar="RE" )
        parser.add_argument("-s", "--snapshot", dest="snapshot", action="count", help="should a snapshot be generated [default: %(default)s]")
        parser.add_argument('-V', '--version', action='version', version=program_version_message)

        # Process arguments
        args = parser.parse_args()

        verbose = args.verbose
        inpattern = args.include
        expattern = args.exclude
        is_snapshot_version = args.snapshot

        if verbose and verbose > 0:
            print('Verbose mode on')

        if AUTH_TOKEN:
            print('Using Auth token string ending %s' % (AUTH_TOKEN[-4:]))
        else:
            print('No AUTH_TOKEN, using unauthenticated access')

        if inpattern and expattern and inpattern == expattern:
            raise CLIError("include and exclude pattern are equal! Nothing will be processed.")

        gh = GitHub(GITHUB_API, token=AUTH_TOKEN)
        prover_repository = gh.repository(JKIND_CHECKER_OWNER, JKIND_CHECKER_REPO)
        jkind_releases = [r for r in prover_repository.releases()]
        jkind_versions = [r.tag_name for r in jkind_releases]
        print('JKIND all versions: %s' % (pformat(jkind_versions)))

        plugin_repository = gh.repository(JKIND_PLUGIN_OWNER, JKIND_PLUGIN_REPO)
        extant_plugin_versions = [r.tag_name for r in plugin_repository.releases()]
        print('Extant plugin versions: %s' % (pformat(extant_plugin_versions)))

        # filter out the versions matching the exclude pattern
        if expattern:
            regex = re.compile(expattern)
            jkind_versions = [x for x in filter(lambda x: not regex.match(x), jkind_versions)]
        print('JKIND versions after exclude: %s' % (pformat(jkind_versions)))

        # filter on include pattern
        if inpattern:
            regex = re.compile(inpattern)
            jkind_versions = [x for x in filter(regex.match, jkind_versions)]
        print('JKIND versions after include: %s' % (pformat(jkind_versions)))

        # Find the plugin version corresponding to the jkind-version
        regex = re.compile(r'\d+\.\d+\.\d+')
        plugin_versions = {regex.search(x).group(0) : x for x in filter(regex.search, jkind_versions)}
        print('Plugin versions matching JKIND versions: %s' % (pformat(plugin_versions)))

        # remove the versions already packaged as plugin
        plugin_versions = {v : plugin_versions[v] for v in filter(lambda x: (not x in extant_plugin_versions), plugin_versions)}
        print('Plugin versions not yet packaged: %s' % (pformat(plugin_versions)))

        build_order = sorted(plugin_versions.keys())
        print('Building plugin versions: %s' % (pformat(build_order)))

        for ver in build_order:
            jkind_tag = plugin_versions[ver]
            regex = re.compile("[Vv](?P<major>\d+)\.(?P<minor>\d+)\.(?P<micro>\d+)(?:[-\.](?P<qualifier>.*))?")
            vmatch = regex.match(jkind_tag)
            vdict = vmatch.groupdict()

            major_version = vdict["major"]
            minor_version = vdict["minor"] if not is_snapshot_version else "%d" % (int(vdict["minor"]) + 1)
            micro_version = vdict["micro"] if not is_snapshot_version else '0'
            canonical_version = "%s.%s.%s" % (major_version, minor_version, micro_version)

            version_has_qualifier = vdict["qualifier"] is not None or is_snapshot_version
            jkind_version = canonical_version + ("-SNAPSHOT" if version_has_qualifier else '')
            bundle_version = canonical_version + (".qualifier" if version_has_qualifier else '')

            print('Building plugin version %s ...' % (jkind_tag))
            release_description = next(filter(lambda r: r.tag_name == jkind_tag, jkind_releases), None)
            package_plugin(jkind_tag, jkind_version, bundle_version, is_snapshot_version, release_description)

        return 0
    except KeyboardInterrupt:
        ### handle keyboard interrupt ###
        return 0
    except Exception as e:
        if DEBUG:
            raise(e)
        indent = len(program_name) * " "
        print(program_name + ": " + repr(e), file=sys.stderr)
        print(indent + "  for help use --help", file=sys.stderr)
        return 2

if __name__ == "__main__":
    sys.exit(main())
