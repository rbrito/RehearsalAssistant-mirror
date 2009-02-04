package urbanstew.RehearsalAssistant;

import android.net.Uri;
import android.provider.BaseColumns;

public class Rehearsal
{
    public static final String AUTHORITY = "com.urbanstew.provider.RehearsalAssistant";

    // This class cannot be instantiated
    private Rehearsal() {}
    

    /**
     * Projects table
     */
    public static final class Projects implements BaseColumns {
        // This class cannot be instantiated
        private Projects() {}

        public static final String TABLE_NAME = "projects";
    }
        
    /**
     * Runs table
     */
    public static final class Sessions implements BaseColumns {
        // This class cannot be instantiated
        private Sessions() {}

        public static final String TABLE_NAME = "sessions";
        /**
         * The content:// style URL for this table
         */
        public static final Uri CONTENT_URI = Uri.parse("content://" + AUTHORITY + "/sessions");

        /**
         * The MIME type of {@link #CONTENT_URI} providing a directory of runs.
         */
        public static final String CONTENT_TYPE = "vnd.android.cursor.dir/vnd.urbanstew.session";

        /**
         * The MIME type of a {@link #CONTENT_URI} sub-directory of a single run.
         */
        public static final String CONTENT_ITEM_TYPE = "vnd.android.cursor.item/vnd.urbanstew.session";

        /**
         * The default sort order for this table
         */
        public static final String DEFAULT_SORT_ORDER = "project_id DESC";

        /**
         * The title of the run
         * <P>Type: TEXT</P>
         */
        public static final String TITLE = "title";
        public static final String PROJECT_ID = "project_id";
        public static final String IDENTIFIER = "identifier";
        public static final String START_TIME = "start_time";
        public static final String END_TIME = "end_time";
    }

    public static final class Annotations implements BaseColumns {
        // This class cannot be instantiated
        private Annotations() {}

        public static final String TABLE_NAME = "annotations";

        /**
         * The content:// style URL for this table
         */
        public static final Uri CONTENT_URI = Uri.parse("content://" + AUTHORITY + "/annotations");

        /**
         * The MIME type of {@link #CONTENT_URI} providing a directory of runs.
         */
        public static final String CONTENT_TYPE = "vnd.android.cursor.dir/vnd.urbanstew.annotation";

        /**
         * The MIME type of a {@link #CONTENT_URI} sub-directory of a single run.
         */
        public static final String CONTENT_ITEM_TYPE = "vnd.android.cursor.item/vnd.urbanstew.annotation";

        /**
         * The default sort order for this table
         */
        public static final String DEFAULT_SORT_ORDER = "start_time ASC";

        /**
         * The run_id of the annotation
         * <P>Type: INTEGER</P>
         */
        public static final String SESSION_ID = "session_id";

        public static final String START_TIME = "start_time";
        public static final String END_TIME = "end_time";
        public static final String FILE_NAME = "file_name";
        public static final String VIEWED = "viewed";
    }

}
