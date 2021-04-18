package org.shnux.photooganizer;

import com.drew.imaging.ImageMetadataReader;
import com.drew.imaging.ImageProcessingException;
import com.drew.metadata.Directory;
import com.drew.metadata.Metadata;
import com.drew.metadata.Tag;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.FileTime;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/** Created by Shirish on 4/17/2021 for learning */
@SpringBootApplication
public class PhotoOrganizerApplication {

	private static final Logger LOG = LogManager.getLogger(PhotoOrganizerApplication.class);
  //  public static final String SOURCE_PATH = "C:\\var\\all_photos\\2015 sorted\\002 Home Docs";
  //  public static final String SOURCE_PATH = "C:\\var\\all_photos\\1";
  public static final String SOURCE_PATH = "C:\\var\\all_photos";
  public static final String DESTINATION_PATH = "C:/tmp/photos/";
  public static final Map<String, String> monthMap =
      Stream.of(
              new String[][] {
                {"Jan", "_01_Jan"},
                {"Feb", "_02_Feb"},
                {"Mar", "_03_Mar"},
                {"Apr", "_04_Apr"},
                {"May", "_05_May"},
                {"Jun", "_06_Jun"},
                {"Jul", "_07_Jul"},
                {"Aug", "_08_Aug"},
                {"Sep", "_09_Sep"},
                {"Oct", "_10_Oct"},
                {"Nov", "_11_Nov"},
                {"Dec", "_12_Dec"}
              })
          .collect(Collectors.toMap(data -> data[0], data -> data[1]));

  public static final Map<String, String> intMonthMap =
      Stream.of(
              new String[][] {
                {"01", "_01_Jan"},
                {"02", "_02_Feb"},
                {"03", "_03_March"},
                {"04", "_04_April"},
                {"05", "_05_May"},
                {"06", "_06_June"},
                {"07", "_07_July"},
                {"08", "_08_Aug"},
                {"09", "_09_Sep"},
                {"10", "_10_Oct"},
                {"11", "_11_Nov"},
                {"12", "_12_Dec"}
              })
          .collect(Collectors.toMap(data -> data[0], data -> data[1]));
  public static final String DONT_KNOW = "dont_know";
  private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy_MMM");

  /**
   * Create folder year->Month
   *
   * <p>Default ::: YYYY_$01_MONTH
   *
   * <p>Special Occasions ::: YYYY_$01_MONTH_DATE_SPECIAL_OCCASIONS
   */
  public static void main(String[] args) {
    // path
    //                createFolders();
    // read photos
    readPhotos(SOURCE_PATH);
  }

  public static void readPhotos(String source) {

    // Creates an array in which we will store the names of files and directories
    String[] pathNames;

    // Creates a new File instance by converting the given pathname string
    // into an abstract pathname
    File f = new File(source);

    // Populates the array with names of files and directories
    pathNames = f.list();

    // For each pathname in the pathNames array
    for (String fileName : pathNames) {
      // Print the names of files and directories
      final String filePath = source + "\\" + fileName;
      boolean isDirectory = new File(filePath).isDirectory();
      if (isDirectory) {
        readPhotos(filePath);
      } else {
        moveFilesInThisPath(fileName, filePath);
      }
    }
  }

  private static void moveFilesInThisPath(String fileName, String filePath) {
    LOG.info("fileName = " + filePath);

    if (fileName.startsWith("VID")) {
      final String yearMonth = getYearMonthFromFileName(fileName);
      //      moveFileToDirectory(filePath, DESTINATION_PATH + "\\" + yearMonth + "\\" + fileName);
    } else {
      try {
        File file = new File(filePath);
        Metadata metadata = ImageMetadataReader.readMetadata(file);
        final String yearMonth = traverseMetadata(metadata, "Using JpegMetadataReader");
        //        moveFileToDirectory(filePath, DESTINATION_PATH + "\\" + yearMonth + "\\" +
        // fileName);
        printAllMetadata(metadata, "Using JpegMetadataReader");
      } catch (ImageProcessingException | IOException e) {
        LOG.info(" Error Processing file:: " + fileName);
        // e.printStackTrace();
      }
    }
  }

  private static String getYearMonthFromFileName(String fileName) {
    String year = fileName.substring(4, 8);
    //    LOG.info("year = " + year);
    String month = fileName.substring(8, 10);
    //    LOG.info("month = " + monthMap.get(month));
    try {
      int yearInt = Integer.parseInt(year);
      LOG.info("year + month = " + year + intMonthMap.get(month));
      return year + intMonthMap.get(month);
    } catch (NumberFormatException e) {
      LOG.info("not an year");
    }
    return DONT_KNOW;
  }

  public static void moveFileToDirectory(String fromFile, String toFile) {

    Path source = Paths.get(fromFile);
    Path target = Paths.get(toFile);

    createFolderIfNotPresent(toFile);

    try {

      // rename or move a file to other path
      // if target exists, throws FileAlreadyExistsException
      //      Files.move(source, target);

      // if target exists, replace it.
      Files.move(source, target, StandardCopyOption.REPLACE_EXISTING);

      // multiple CopyOption
      /*CopyOption[] options = { StandardCopyOption.REPLACE_EXISTING,
                          StandardCopyOption.COPY_ATTRIBUTES,
                          LinkOption.NOFOLLOW_LINKS };

      Files.move(source, target, options);*/

    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  public static void createFolderIfNotPresent(String toFile) {
    File newFolder = new File(toFile).getParentFile();
    try {
      Path path = Paths.get(newFolder.getPath());
      if (!Files.exists(path, LinkOption.NOFOLLOW_LINKS)) {
        Files.createDirectories(path);
        return;
      }
      // java.nio.file.Files;
    } catch (Exception e) {
      LOG.info("e = " + e);
    }
  }

  /** Write all extracted values to stdout. */
  private static String traverseMetadata(Metadata metadata, String method) {
    //
    // A Metadata object contains multiple Directory objects
    //
    for (Directory directory : metadata.getDirectories()) {

      //
      // Each Directory stores values in Tag objects
      //
      for (Tag tag : directory.getTags()) {

        if ("Exif IFD0".equalsIgnoreCase(tag.getDirectoryName())
            && "Date/Time Original".equalsIgnoreCase(tag.getTagName())) {
          return formatStringDate(tag.getDescription());
        }

        if ("Exif SubIFD".equalsIgnoreCase(tag.getDirectoryName())
            && "Date/Time Original".equalsIgnoreCase(tag.getTagName())) {
          return formatStringDate(tag.getDescription());
        }

        //        if ("MP4 Video".equalsIgnoreCase(tag.getDirectoryName())
        //        && "Creation Time".equalsIgnoreCase(tag.getTagName())){
        ////          return formatStringDateLong(tag.getDescription());
        //          return getDate(tag.getDescription());
        //        }
      }

      //
      // Each Directory may also contain error messages
      //
      for (String error : directory.getErrors()) {
        System.err.println("ERROR: " + error);
      }
    }
    return DONT_KNOW;
  }

  private static String formatStringDate(String dateString) {
    String myYearFormat = "yyyy";
    String myMonthFormat = "MMM";
    String yearString = "", monthString = "";

    try {
      DateFormat formatter = new SimpleDateFormat("yyyy:MM:dd hh:mm:ss");
      Date date = formatter.parse(dateString);
      SimpleDateFormat yearFormat = new SimpleDateFormat(myYearFormat);
      yearString = yearFormat.format(date);
      SimpleDateFormat monthFormat = new SimpleDateFormat(myMonthFormat);
      monthString = monthFormat.format(date);
      LOG.info("monthString = " + yearString + monthMap.get(monthString));
      return yearString + monthMap.get(monthString);
    } catch (Exception e) {
      return DONT_KNOW;
    }
  }

  public static String formatDateTime(FileTime fileTime) {

    LocalDateTime localDateTime =
        fileTime.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();

    return localDateTime.format(DATE_FORMATTER);
  }

  private static String formatStringDateLong(String dateString) {
    String myYearFormat = "yyyy";
    String myMonthFormat = "MMM";
    String yearString = "", monthString = "";

    try {
      DateFormat formatter = new SimpleDateFormat("EEE MMM dd HH:mm:ss Z yyyy");
      Date date = formatter.parse(dateString);
      SimpleDateFormat yearFormat = new SimpleDateFormat(myYearFormat);
      yearString = yearFormat.format(date);
      SimpleDateFormat monthFormat = new SimpleDateFormat(myMonthFormat);
      monthString = monthFormat.format(date);
      LOG.info("monthString = " + yearString + monthMap.get(monthString));
      return yearString + monthMap.get(monthString);
    } catch (Exception e) {

    }
    return null;
  }

  /**
   * Don't worry if the folder are present with same name , it will not delete the contents of
   * folder , it will just create the new once which are not present
   */
  public static void createFolders() {
    String pathString = DESTINATION_PATH;
    LOG.info("path = " + pathString);
    // start year
    int startYear = 2008;
    LOG.info("startYear = " + startYear);
    // end year
    int endYear = 2021;
    LOG.info("endYear = " + endYear);

    for (int i = startYear; i < endYear; i++) {
      for (Entry<String, String> ssEntry : monthMap.entrySet()) {
        LOG.info("ssEntry = " + ssEntry);
        final String folderName = i + ssEntry.getValue();
        LOG.info("monthMap[j] = " + folderName);
        try {

          Path path = Paths.get(pathString + folderName);

          // java.nio.file.Files;
          Files.createDirectories(path);

          LOG.info("Directory is created!");

        } catch (IOException e) {

          System.err.println("Failed to create directory!" + e.getMessage());
        }
      }
    }
  }

  /** Write all extracted values to stdout. */
  private static void printAllMetadata(Metadata metadata, String method) {
    LOG.info("-------------------------------------------------");
    System.out.print(' ');
    System.out.print(method);
    LOG.info("-------------------------------------------------");

    //
    // A Metadata object contains multiple Directory objects
    //
    for (Directory directory : metadata.getDirectories()) {

      //
      // Each Directory stores values in Tag objects
      //
      for (Tag tag : directory.getTags()) {
        LOG.info(tag.toString());
      }

      //
      // Each Directory may also contain error messages
      //
      for (String error : directory.getErrors()) {
        System.err.println("ERROR: " + error);
      }
    }
  }

  private static void print(Exception exception) {
    System.err.println("EXCEPTION: " + exception);
  }
}
