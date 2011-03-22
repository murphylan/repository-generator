package org.sonatype.spice.generator;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.Properties;
import java.util.Random;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;

public class RepositoryGeneratorMain
{
    public static final java.util.TimeZone TS_TZ = java.util.TimeZone.getTimeZone( "UTC" );

    public static final java.text.DateFormat TS_FORMAT = new java.text.SimpleDateFormat( "yyyyMMdd.HHmmss" );

    static
    {
        TS_FORMAT.setTimeZone( TS_TZ );
    }

    private static final String LINE_ENDING = System.getProperty( "line.separator" );

    private static Properties props = null;

    private static final String DEFAULT_GROUP_IDS = "group1,group2,group3";

    private static final String DEFAULT_ARTIFACT_IDS = "artifact1,artifact2,artifact3";

    private static final String DEFAULT_VERSIONS = "UNDEFINED";

    private static final String DEFAULT_GROUP_IDS_UNDEFINED_COUNT = "10";

    private static final String DEFAULT_ARTIFACT_IDS_UNDEFINED_COUNT = "10";

    private static final String DEFAULT_VERSION_UNDEFINED_COUNT = "500";

    private static final String DEFAULT_APPEND_OUTPUT = "false";

    private static final String DEFAULT_OUTPUT_DIR = "./output";

    private static final String KEY_GROUP_IDS = "group-id-values";

    private static final String KEY_ARTIFACT_IDS = "artifact-id-values";

    private static final String KEY_VERSIONS = "version-values";

    private static final String KEY_GROUP_IDS_UNDEFINED_COUNT = "group-id-undefined-count";

    private static final String KEY_ARTIFACT_IDS_UNDEFINED_COUNT = "artifact-id-undefined-count";

    private static final String KEY_VERSIONS_UNDEFINED_COUNT = "version-undefined-count";

    private static final String KEY_APPEND_OUTPUT = "append-output";

    private static final String KEY_OUTPUT_DIR = "output-directory";

    private static final String PROPERTIES_FILE = "/repository-generator.properties";

    private static final String PRE_POM = "<project" + LINE_ENDING + "    xmlns=\"http://maven.apache.org/POM/4.0.0\""
        + LINE_ENDING + "    xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"" + LINE_ENDING
        + "    xsi:schemaLocation=\"http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd\">"
        + LINE_ENDING + "    <modelVersion>4.0.0</modelVersion>" + LINE_ENDING;

    private static final String POST_POM = "</project>" + LINE_ENDING;

    public static void main( String[] args )
        throws Exception
    {
        // now lets get our set of groupIds, artifactIds and versions
        Set<String> groupIds =
            stringToSet( getPropertyValue( KEY_GROUP_IDS, DEFAULT_GROUP_IDS ),
                Integer.parseInt( getPropertyValue( KEY_GROUP_IDS_UNDEFINED_COUNT, DEFAULT_GROUP_IDS_UNDEFINED_COUNT ) ) );
        Set<String> artifactIds =
            stringToSet( getPropertyValue( KEY_ARTIFACT_IDS, DEFAULT_ARTIFACT_IDS ),
                Integer.parseInt( getPropertyValue( KEY_ARTIFACT_IDS_UNDEFINED_COUNT,
                    DEFAULT_ARTIFACT_IDS_UNDEFINED_COUNT ) ) );
        Set<String> versions =
            stringToSet( getPropertyValue( KEY_VERSIONS, DEFAULT_VERSIONS ),
                Integer.parseInt( getPropertyValue( KEY_VERSIONS_UNDEFINED_COUNT, DEFAULT_VERSION_UNDEFINED_COUNT ) ) );

        // now lets combine all these values into single list of items to create
        Set<String> gavs = getGAVStrings( groupIds, artifactIds, versions );

        // now lets start generating some stuff
        generateGAVRepo( gavs, new File( getPropertyValue( KEY_OUTPUT_DIR, DEFAULT_OUTPUT_DIR ) ),
            Boolean.parseBoolean( getPropertyValue( KEY_APPEND_OUTPUT, DEFAULT_APPEND_OUTPUT ) ) );
    }

    private static String getPropertyValue( String key, String defaultValue )
        throws IOException
    {
        if ( props == null )
        {
            props = new Properties();

            props.load( RepositoryGeneratorMain.class.getResourceAsStream( PROPERTIES_FILE ) );
        }

        String value = System.getProperty( key );

        if ( value != null )
        {
            return value;
        }

        return props.getProperty( key, defaultValue );
    }

    private static Set<String> stringToSet( String values, int undefinedCount )
    {
        Set<String> valuesSet = new HashSet<String>();

        if ( "UNDEFINED".equals( values ) )
        {
            // if undefined, we'll add XX items
            for ( int i = 0; i < undefinedCount; i++ )
            {
                valuesSet.add( Integer.toString( i ) );
            }
        }
        else
        {
            String[] valuesArray = values.split( "," );

            for ( int i = 0; i < valuesArray.length; i++ )
            {
                valuesSet.add( valuesArray[i] );
            }
        }

        return valuesSet;
    }

    private static Set<String> getGAVStrings( Set<String> groupIds, Set<String> artifactIds, Set<String> versions )
    {
        Set<String> gavs = new HashSet<String>();

        for ( String groupId : groupIds )
        {
            for ( String artifactId : artifactIds )
            {
                for ( String version : versions )
                {
                    gavs.add( groupId + ":" + artifactId + ":" + version );
                }
            }
        }

        return gavs;
    }

    private static void generateGAVRepo( Set<String> gavs, File baseOutputDir, boolean append )
        throws IOException
    {
        if ( !append )
        {
            baseOutputDir.delete();
        }

        int i = 1;

        for ( String gav : gavs )
        {
            String[] parts = gav.split( ":" );
            String groupId = parts[0];
            String artifactId = parts[1];
            String version = parts[2];

            String relativePath = groupId.replace( ".", "/" ) + "/" + artifactId + "/" + version + "-SNAPSHOT";

            File artifactDir = new File( baseOutputDir, relativePath );
            artifactDir.mkdirs();

            int snapshots = (int) ( Math.random() * 10 );
            for ( int j = 0; j < snapshots; j++ )
            {
                Random r = new Random();
                Calendar cdr = Calendar.getInstance();
                cdr.set( Calendar.HOUR_OF_DAY, j + 1 );
                cdr.set( Calendar.MINUTE, r.nextInt( 59 ) );
                cdr.set( Calendar.SECOND, r.nextInt( 59 ) );
                long val1 = cdr.getTimeInMillis();

                Date d = new Date( val1 );
                String v = version + "-" + TS_FORMAT.format( d ) + "-" + ( j + 1 );

                generateJar( groupId, artifactId, v, artifactDir, i, null );
                generateJar( groupId, artifactId, v, artifactDir, i, "tests" );
                generateJar( groupId, artifactId, v, artifactDir, i, "sources" );
                generateJar( groupId, artifactId, v, artifactDir, i, "javadoc" );
                generateJar( groupId, artifactId, v, artifactDir, i, "site" );
                generatePom( groupId, artifactId, v, artifactDir );

            }
            System.out.println( "Generated GAV #" + i + " " + gav );
            i++;
        }
    }

    private static void generateJar( String groupId, String artifactId, String version, File dir, int i,
                                     String classifier )
        throws IOException
    {
        JarOutputStream jos = null;

        try
        {
            String c = classifier == null ? "" : "-" + classifier;
            jos =
                new JarOutputStream( new FileOutputStream( new File( dir, artifactId + "-" + version + c + ".jar" ) ) );
            jos.putNextEntry( new JarEntry( "test/" ) );
            jos.closeEntry();

            if ( classifier == null )
            {
                jos.putNextEntry( new JarEntry( "META-INF/maven/plugin.xml" ) );
                String pluginXml =
                    "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" + "<plugin>\n" + "  <name>" + artifactId + "-"
                        + version + "</name>\n" + "  <description>" + groupId + "-" + artifactId + "-" + version
                        + "</description>\n" + "  <groupId>" + groupId + "</groupId>\n" + "  <artifactId>" + artifactId
                        + "</artifactId>\n" + "  <version>" + version + "</version>\n" + "  <goalPrefix>" + artifactId
                        + "</goalPrefix>\n" + "</plugin>\n";
                jos.write( pluginXml.getBytes( "UTF-8" ) );
                jos.closeEntry();
            }
        }
        finally
        {
            if ( jos != null )
            {
                jos.close();
            }
        }
    }

    private static void generatePom( String groupId, String artifactId, String version, File dir )
        throws IOException
    {
        String pom = PRE_POM;
        pom += "    <groupId>" + groupId + "</groupId>" + LINE_ENDING;
        pom += "    <artifactId>" + artifactId + "</artifactId>" + LINE_ENDING;
        pom += "    <version>" + version + "</version>" + LINE_ENDING;
        pom += "    <packaging>maven-plugin</packaging>" + LINE_ENDING;
        pom += POST_POM;

        InputStream is = null;
        OutputStream os = null;

        try
        {
            is = new ByteArrayInputStream( pom.getBytes( "UTF-8" ) );
            os = new FileOutputStream( new File( dir, artifactId + "-" + version + ".pom" ) );

            byte buf[] = new byte[1024];
            int len;
            while ( ( len = is.read( buf ) ) > 0 )
            {
                os.write( buf, 0, len );
            }
        }
        finally
        {
            if ( is != null )
            {
                is.close();
            }

            if ( os != null )
            {
                os.close();
            }
        }
    }
}
