#!/usr/bin/python

from common import dropbox_client
from dropbox import rest

import sys
import os
import datetime

# dropbox API doesn't return any sensible datestrings.
DATE_FORMAT = "%a, %d %b %Y %H:%M:%S +0000"
INPUT_DATE_FORMAT = "%d%m%Y"

MAX_DAYS=15

if len(sys.argv) not in (5,6):
    print "Usage: undelete.py <LIST|UNDELETE> <from date> <to date> <recovery folder> [<root path>]"
    print "    option:"
    print "        LIST               list all files which are deleted in the start folder"
    print "        UNDELETE           recover all deleted files into the output folder"
    print "    from date              from date to start the recovery or listing (format: ddmmyyyy)"
    print "    to date                to date to start the recovery or listing (format: ddmmyyyy)"
    print "    recovery folder        where to recover the deleted files"
    print "    root path              Optional, indicates where to start the walk"
    sys.exit(1)

# if len(sys.argv) not in (3, 5, 7, 9) or sys.argv[1] not in ("LIST", "UNDELETE"):
#     print "Usage: undelete.py <option> <output folder> [-s <start walk>] [-f <from date>] [-t <to date>]"
#     print "    option:"
#     print "        LIST               list all files which are deleted in the start folder"
#     print "        UNDELETE           recover all deleted files into the output folder"
#     print "    -f                     from date to start the recovery or listing"
#     print "    -t                     to date to start the recovery or listing"
#     sys.exit(1)
recover_to = sys.argv[4]
try:
    start_walk = sys.argv[5]
except IndexError:
    start_walk = "/"
USE_RESTORE = sys.argv[1] == "UNDELETE"
from_date = datetime.datetime.strptime(sys.argv[2], INPUT_DATE_FORMAT)
to_date = datetime.datetime.strptime(sys.argv[3], INPUT_DATE_FORMAT)

client = dropbox_client()


def recover_tree(folder = "/", recover_to=recover_to, from_date=None, to_date=None):
    # called recursively. We're going to walk the entire Dropbox
    # file tree, starting at 'folder', files first, and recover anything
    # deleted in the last 5 days.
    print "walking in %s"%folder

    try:
        meta = client.metadata(folder, include_deleted=True, file_limit=10000)
    except rest.ErrorResponse, e:
        print e # normally "too many files". Dropbox will only list 10000 files in
        # a folder.
        return
    
    # walk files first, folders later
    for filedata in filter(lambda f: not f.get("is_dir", False), meta["contents"]):
        # we only care about deleted files.
        if not filedata.get("is_deleted", False):
            continue

        # this is the date the file was deleted on
        date = datetime.datetime.strptime(filedata["modified"], DATE_FORMAT)
        if from_date is not None and from_date > date:
            continue
        if to_date is not None and to_date+datetime.timedelta(days=1) < date:
            continue

        # this is where we'll restore it to.
        target = os.path.join(recover_to, filedata["path"][1:])

        if os.path.exists(target):
            # already recovered
            pass
        else:
            print "  %s is deleted"%(filedata["path"])

            # fetch file history, and pick the first non-deleted revision.
            revisions = client.revisions(filedata["path"], rev_limit=10)
            alive = filter(lambda r: not r.get("is_deleted", False), revisions)[0]

            # create destination folder.
            try:
                os.makedirs(os.path.dirname(target))
            except OSError:
                pass

            if USE_RESTORE:
                try:
                    fh = client.get_file(filedata["path"], rev=alive["rev"])
                    with open(target+".temp", "w") as oh:
                        oh.write(fh.read())
                    os.rename(target+'.temp', target)
                    print "    ..recovered"
                except Exception, e:
                    print "*** RECOVERY FAILED: %s"%e


    # now loop over the folders and recursively walk into them. Folders can
    # be deleted too, but don't try to undelete them, we'll rely on them being
    # implicitly reinflated when their files are restored.
    for file in filter(lambda f: f.get("is_dir", False), meta["contents"]):
        recover_tree(file["path"], recover_to, from_date, to_date)


recover_tree(folder=start_walk, from_date=from_date, to_date=to_date)
