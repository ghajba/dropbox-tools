package biz.hahamo.dev.undelete;

import java.io.BufferedReader;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

import org.apache.commons.lang3.time.DateUtils;

import com.dropbox.core.DbxAppInfo;
import com.dropbox.core.DbxAuthFinish;
import com.dropbox.core.DbxClient;
import com.dropbox.core.DbxEntry;
import com.dropbox.core.DbxEntry.File;
import com.dropbox.core.DbxException;
import com.dropbox.core.DbxRequestConfig;
import com.dropbox.core.DbxWebAuthNoRedirect;

public class Undelete {

    public static void main(String... args) throws IOException, DbxException {
        final String APP_KEY = "3480tznbkyol9n7";
        final String APP_SECRET = "r56nvj88xqnb15k";

        if (args.length < 4 || args.length > 5 || (!"LIST".equalsIgnoreCase(args[0]) && !"UNDELETE".equals(args[0]))) {
            printHelp(System.out);
            System.exit(1);
        }
        boolean undelete = "UNDELETE".equals(args[0]);
        Date fromDate = null;
        Date toDate = null;
        String startFolder = args.length == 5 ? args[4] : "/";
        String recoveryFolder = args[3];
        try {
            fromDate = new SimpleDateFormat("yyyyMMdd").parse(args[1]);
            toDate = new SimpleDateFormat("yyyyMMdd").parse(args[2]);
        }
        catch (ParseException e) {
            printHelp(System.out);
            System.exit(2);
        }

        DbxAppInfo appInfo = new DbxAppInfo(APP_KEY, APP_SECRET);

        DbxRequestConfig config = new DbxRequestConfig("UndoUtils/1.0", Locale.getDefault().toString());
        DbxWebAuthNoRedirect webAuth = new DbxWebAuthNoRedirect(config, appInfo);

        String authorizeUrl = webAuth.start();
        System.out.println("1. Go to: " + authorizeUrl);
        System.out.println("2. Click \"Allow\" (you might have to log in first)");
        System.out.println("3. Copy the authorization code.");
        String code = new BufferedReader(new InputStreamReader(System.in)).readLine().trim();

        // This will fail if the user enters an invalid authorization code.
        DbxAuthFinish authFinish = webAuth.finish(code);
        String accessToken = authFinish.accessToken;

        DbxClient client = new DbxClient(config, accessToken);

        System.out.println("Linked account: " + client.getAccountInfo().displayName);

        recoverTree(client, startFolder, recoveryFolder, fromDate, toDate, undelete);
    }

    private static void recoverTree(DbxClient client, String folder, String recoverTo, Date fromDate, Date toDate,
            boolean undelete) {
        System.out.println("Walking in " + folder);
        DbxEntry.WithChildren metadata = null;
        try {
            metadata   = client.getMetadataWithChildren(folder, true);
        }
        catch (DbxException e) {
            e.printStackTrace();
            System.exit(3);
        }
        for (DbxEntry child : metadata.children) {
            if(child.isFolder() || !(child instanceof File)) {
                continue;
            }
            File file = ((File)child);
            if(!file.deleted) {
                continue;
            }
            if(fromDate.after(DateUtils.truncate(file.lastModified, Calendar.DAY_OF_MONTH))
                    || toDate.before(DateUtils.truncate(toDate, Calendar.DAY_OF_MONTH))) {
                continue;
            }
            java.io.File newFile = new java.io.File(recoverTo, child.path);
            if(newFile.exists()) {
                continue;
            }
            System.out.println(String.format("%s is deleted", child.path));
            if(!undelete) {
                continue;
            }
            newFile.getParentFile().mkdirs();
            String revision = getLatestRevision(client, file);
            
            try {
                System.out.println("  Starting recovery... ");
                client.getFile(child.path, revision, new FileOutputStream(newFile));
            }
            catch (DbxException | IOException e) {
                e.printStackTrace();
                continue;
            }
            System.out.println("     ... recovered");
        }
        for (DbxEntry child : metadata.children) {
            if(child.isFolder()) {
                recoverTree(client, child.path, recoverTo, fromDate, toDate, undelete);
            }
        }

    }

    private static String getLatestRevision(DbxClient client, File file) {
        try {
            for(File f : client.getRevisions(file.path)) {
                if(!f.deleted) {
                    return f.rev;
                }
            }
        }
        catch (DbxException e) {
            e.printStackTrace();
        }
        return null;
    }

    private static void printHelp(PrintStream out) {
        out.println("Usage: undelete <LIST|UNDELETE> <from date> <to date> <recovery folder> [<root path>]");
        out.println();
        out.println("    option:");
        out.println("        LIST               list all files which are deleted in the start folder");
        out.println("        UNDELETE           recover all deleted files into the output folder");
        out.println("    from date              from date to start the recovery or listing in the format yyyyMMdd");
        out.println("    to date                to date to start the recovery or listing in the format yyyyMMdd");
        out.println("    recovery folder        where to recover the deleted files");
        out.println("    root path              Optional, indicates where to start the walk");
    }

}
