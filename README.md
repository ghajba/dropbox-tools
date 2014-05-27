# Dropbox Recovery Tools

I forked this repository to adapt the solution to my preferences. The usage is almost the same as it was.

However I had to change the common.py and add my Application's key and secret to it because the original version was not enabled for public use.
Perhaps you have to do the same and create an application on Dropbox and add that's key and secret to the application.

# My Tools

You can use either the Python script undelete.py (where you have a dependency on common.py and have to install oauth and dropbox libraries listed in the "requirements.txt") or use the single executable  "undelete" (currently for Mac OS but I plan Windows and Linux versions too).
The argument list is needed for the single executable too.

## undelete.py
Usage:

    python undelete.py <LIST|UNDELETE> <from date> <to date> <recovery folder> [<root path>]
eg:
    python undelete.py UNDELETE 18042014 20042014 ./recovery /HomoErraticus

The date format is currently ddMMyyyy.
The option LIST lists all deleted files in the given time interval without any recovery.
The option UNDELETE recovers all deleted files in the given time interval into the recovery folder -- you have to copy it to your Dropbox folder again if you want to sync them again.
It walks the remote Dropbox repository starting with the <root path> if no <root path> is specified, the script walks the whole Dropbox structure.
If the file already exists in the recovery folder it will not be overridden.


# Original description from the forked repository:
http://movieos.org/code/dropbox-tools/

A collection of little utilities I'm building as and when I need them to fix
things that are wrong with my Dropbox. Dropbox is great and I could not live without
it, but this is the Real World and things break.

Just to be clear - you should _NOT_ use these tools without reading and
understanding them. They're 30 lines of code each, and full of comments, but
they perform potentially destructive changes to your Dropbox folder.

# tools

## `zero_length.py`

Recover truncated files.

Usage:

    $ python zero_length.py <Dropbox folder>

Something weird happened to a shared dropbox I'm part of, and lots of files
became replaced by zero-length versions of themselves. Clearly this is annoying.
This script walks through the Dropbox folder, finds zero-length files, and
recovers them to the first state in their history where they were _not_ zero
length.

## `bulk_undelete.py`

Bulk-undelete files

Usage:

    zero_length.py <recovery folder> [<root path>]

eg:

    $ python zero_length.py ~/DropboxRecovery/ /SharedThings/

This was my first tool. I managed to delete 18,000 files from the shared work
Dropbox folder. Although in theory Dropbox lets you undelete files, in practice
you need to do this one at a time. Not going to happen. This script will walk
the remote Dropbox repository, and download the most recent version of any
file that was deleted in the last 5 days into the recovery folder. If the file
already exists in the target folder, it won't be overwritten, so the script will
only create new files.

I suggest you recover into a new, empty folder, then copy the files back into
dropbox once you're happy that it worked.


# instructions

Install requirements.

    $ pip install -r requrements.txt

Run script.

    # python zero_length.py ~/Dropbox

Eat bacon.


