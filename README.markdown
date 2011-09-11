#XKnow

XKnow is a daily data logging tool for Android.  It can be
accessed on the Android Market.

#History

XKnow started life as EvenTrend, a project by Barclay Osborn.  With his
approval, the original Subversion repository at
http://code.google.com/p/eventrend/ was forked in September 2011 to its current
home on Github.

Because of key management problems, the continuing development could not
directly replace the original in the Android Market.  A new name was needed.  The new developers chose
XKnow because no projects at the time were using it and it can be interpreted as:

* Extended knowledge
* Know x (where x stands for anything one would like to know)

#Developing

The branch structure is described in: http://nvie.com/posts/a-successful-git-branching-model/  

A quick summary:

##Permanent branches

* master - current production version - HEAD is always in a production
           ready state.  Each commit (except ones to just update the github readme) is a new release.  A tag is made
           with the release id at each commit.

* develop - work in progress on next release

##Supporting branches

All merges that are the end of a supporting branch's life-cycle are `git merge --no-ff`

### feature

* Branch from: develop

* Merge to: develop

* Named: anything except master, develop, release-*, or hotfix-*

Local scope.

### release 

* Branch from: develop

* Merge to: develop and master

* Named: release-*

Commit bugfixes only - NO FEATURES.

Remember to bump the version number.

### hotfix

* Branch from: master

* Merge to: develop and master (and any extant release branch)

* Named: hotfix-*

For fixing immediate bugs when develop is still too unstable to
release.  Remember to bump the version number.
