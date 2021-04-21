package org.shnux.photooganizer;

import com.drew.imaging.ImageMetadataReader;
import com.drew.imaging.ImageProcessingException;
import com.drew.metadata.Directory;
import com.drew.metadata.Metadata;
import com.drew.metadata.Tag;
import com.sampullara.cli.Args;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.FileTime;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/** Created by Shirish on 4/17/2021 for learning */
public class PhotoOrganizerApplication {

  public static final String PATH_SEPARATOR = File.separator;
  protected static final Map<String, String> monthMap =
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
  protected static final Map<String, String> intMonthMap =
      Stream.of(
              new String[][] {
                {"01", "_01_Jan"},
                {"02", "_02_Feb"},
                {"03", "_03_Mar"},
                {"04", "_04_Apr"},
                {"05", "_05_May"},
                {"06", "_06_Jun"},
                {"07", "_07_Jul"},
                {"08", "_08_Aug"},
                {"09", "_09_Sep"},
                {"10", "_10_Oct"},
                {"11", "_11_Nov"},
                {"12", "_12_Dec"}
              })
          .collect(Collectors.toMap(data -> data[0], data -> data[1]));
  public static final String DONT_KNOW = "dont_know";
  private static final Logger LOG = LogManager.getLogger(PhotoOrganizerApplication.class);
  private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy_MMM");

  /**
   * Create folder year->Month
   *
   * <p>Default ::: YYYY_$01_MONTH
   *
   * <p>Special Occasions ::: YYYY_$01_MONTH_DATE_SPECIAL_OCCASIONS
   */
  public static void main(String[] args) {

    if (Objects.nonNull(Options.source) && Objects.nonNull(Options.destination)) {
      readPhotos(Options.source);
    } else {
      LOG.info("Please provide source and destination");
    }
  }

  public static void readPhotos(String source) {

    // Creates an array in which we will store the names of files and directories
    String[] pathNames;

    // Creates a new File instance by converting the given pathname string
    // into an abstract pathname
    File f = new File(source);

    // Populates the array with names of files and directories
    pathNames = f.list();

    LOG.info("PATH_SEPARATOR = {}", PATH_SEPARATOR);
    // For each pathname in the pathNames array
    for (String fileName : pathNames) {
      // Print the names of files and directories
      final String filePath = source + PATH_SEPARATOR + fileName;
      boolean isDirectory = new File(filePath).isDirectory();
      if (isDirectory) {
        readPhotos(filePath);
      } else {
        //        moveFilesInThisPath(fileName, filePath);
        printFilesInThisPath(fileName, filePath);
      }
    }
  }

  private static void moveFilesInThisPath(String fileName, String filePath) {
    LOG.info("fileName = " + filePath);

    if (fileName.startsWith("VID")) {
      final String yearMonth = getYearMonthFromFileName(fileName);
      moveFileToDirectory(
          filePath, Options.destination + PATH_SEPARATOR + yearMonth + PATH_SEPARATOR + fileName);
    } else {
      try {
        File file = new File(filePath);
        Metadata metadata = ImageMetadataReader.readMetadata(file);
        final String yearMonth = traverseMetadata(metadata, "Using JpegMetadataReader");
        moveFileToDirectory(
            filePath, Options.destination + PATH_SEPARATOR + yearMonth + PATH_SEPARATOR + fileName);
        //        printAllMetadata(metadata, "Using JpegMetadataReader");
      } catch (ImageProcessingException | IOException e) {
        LOG.error(" Error Processing file:: {}" , fileName);
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
      Files.move(source, target);

      // if target exists, replace it.
      //      Files.move(source, target, StandardCopyOption.REPLACE_EXISTING);

      // multiple CopyOption
      /*CopyOption[] options = { StandardCopyOption.REPLACE_EXISTING,
                          StandardCopyOption.COPY_ATTRIBUTES,
                          LinkOption.NOFOLLOW_LINKS };

      Files.move(source, target, options);*/

    } catch (IOException e) {
      // e.printStackTrace();
      LOG.error("Already exists in Destination {}", target);
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

        if (tag.getDirectoryName().contains("Video")
            && "Creation Time".equalsIgnoreCase(tag.getTagName())) {
          formatStringDateLong(tag.getDescription());
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
        LOG.error("ERROR: " + error);
      }
    }
    return DONT_KNOW;
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
      LOG.error("e = " + e);
    }
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

  private static String formatStringDateLong(String dateString) {
    SimpleDateFormat yearFormat = new SimpleDateFormat("yyyy"), monthFormat = new SimpleDateFormat("MMM");

    try {
//      String time = "Fri Sep 15 13:27:54 +05:30 2017";
      DateFormat inputFormat = new SimpleDateFormat("E MMM dd HH:mm:ss XXX yyyy", Locale.ROOT);
      Date date = inputFormat.parse(dateString);
      System.out.println(date);
      String yearMonth = yearFormat.format(date) + monthMap.get(monthFormat.format(date));
      LOG.info("yearMonth = " + yearMonth);
      return yearFormat.format(date) + monthMap.get(monthFormat.format(date));
    } catch (Exception e) {

    }
    return null;
  }

  private static void printFilesInThisPath(String fileName, String filePath) {
    LOG.info("fileName = " + filePath);

    try {
      File file = new File(filePath);
      Metadata metadata = ImageMetadataReader.readMetadata(file);
      final String yearMonth = traverseMetadata(metadata, "Using JpegMetadataReader");
      printAllMetadata(metadata, "Using JpegMetadataReader");
    } catch (ImageProcessingException | IOException e) {
      LOG.error(" Error Processing file:: " + fileName);
    }
  }

  public static String formatDateTime(FileTime fileTime) {

    LocalDateTime localDateTime =
        fileTime.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();

    return localDateTime.format(DATE_FORMATTER);
  }

  /**
   * Don't worry if the folder are present with same name , it will not delete the contents of
   * folder , it will just create the new once which are not present
   */
  public static void createFolders() {
    String pathString = Options.destination;
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

          LOG.error("Failed to create directory!" + e.getMessage());
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
        LOG.error("ERROR: " + error);
      }
    }
  }

  private static void print(Exception exception) {
    System.err.println("EXCEPTION: " + exception);
  }
}
