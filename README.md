# JKind-Plugin

Packages the JKind model checker into an Eclipse Plugin for convenient
use in Eclipse-based analysis tools.

## Updating

All of the steps necessary to fetch updated versions of JKind,
package into Eclipse plugins, and deploy these as an Eclipse P2 update
site are codified in the Python script `jkind_fetcher.py` in the
root directory of this repository.

Further, a Travis CI configuration `.travis.yml` is used to
periodically query for new versions of JKind and package these.
Note that any Travis CI build jobs should be set up as periodic rather
than responding to checkins.  This is because the script itself checks
in newly packaged plugins.  Responding to checkins would then fork
bomb Travis CI until no new versions of JKind are found (which should be
pretty quickly, so this isn't quite as bad as it sounds).
