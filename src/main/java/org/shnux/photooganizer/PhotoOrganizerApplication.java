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
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/** Created by Shirish on 4/17/2021 for learning */
public class PhotoOrganizerApplication {

  public static final String PATH_SEPARATOR = File.separator;
  public static final String DONT_KNOW = "dont_know";
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
    Args.parseOrExit(Options.class, args);
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
    if (pathNames != null) {
      for (String fileName : pathNames) {
        // Print the names of files and directories
        final String filePath = source + PATH_SEPARATOR + fileName;
        boolean isDirectory = new File(filePath).isDirectory();
        if (isDirectory) {
          System.out.print(".");
          readPhotos(filePath);
        } else {
          System.out.print("+");
          moveFilesInThisPath(fileName, filePath);
          //        printFilesInThisPath(fileName, filePath);
        }
      }
    }
  }

  private static void moveFilesInThisPath(String fileName, String filePath) {
    LOG.info("fileName = {}", filePath);

    try {
      File file = new File(filePath);
      Metadata metadata = ImageMetadataReader.readMetadata(file);
      final String yearMonth = traverseMetadata(metadata);
      LOG.info(
          "Destination = {}",
          Options.destination + PATH_SEPARATOR + yearMonth + PATH_SEPARATOR + fileName);
      moveFileToDirectory(
          filePath, Options.destination + PATH_SEPARATOR + yearMonth + PATH_SEPARATOR + fileName);
    } catch (ImageProcessingException | IOException e) {
      LOG.error(" Error Processing file:: {}", fileName);
    } catch (Exception e) {
      LOG.error("Generic Error Processing file:: {}", fileName);
    }
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
      LOG.error("Already exists in Destination {}", target);
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
    } catch (Exception e) {
      LOG.error("e = {}", e.getMessage());
    }
  }

  private static void printFilesInThisPath(String fileName, String filePath) {
    LOG.info("fileName {} ", filePath);

    try {
      File file = new File(filePath);
      Metadata metadata = ImageMetadataReader.readMetadata(file);
      final String yearMonth = traverseMetadata(metadata);
      printAllMetadata(metadata);
    } catch (ImageProcessingException | IOException e) {
      LOG.error(" Error Processing file:: " + fileName);
    }
  }

  /** Write all extracted values to stdout. */
  private static String traverseMetadata(Metadata metadata) {
    //
    // A Metadata object contains multiple Directory objects
    //
    for (Directory directory : metadata.getDirectories()) {

      //
      // Each Directory stores values in Tag objects
      //
      for (Tag tag : directory.getTags()) {

        if ("File Modified Date".equalsIgnoreCase(tag.getTagName())) {
          return formatStringDateLong(tag.getDescription());
        }
      }
    }
    return DONT_KNOW;
  }

  /** Write all extracted values to stdout. */
  private static void printAllMetadata(Metadata metadata) {
    LOG.info("-------------------------------------------------");
    System.out.print(' ');
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
        LOG.error("ERROR: {}", error);
      }
    }
  }

  private static String formatStringDateLong(String dateString) {
    SimpleDateFormat yearFormat = new SimpleDateFormat("yyyy"),
        monthFormat = new SimpleDateFormat("MMM");

    try {
      //      String time = "Fri Sep 15 13:27:54 +05:30 2017";
      DateFormat inputFormat = new SimpleDateFormat("E MMM dd HH:mm:ss XXX yyyy", Locale.ROOT);
      Date date = inputFormat.parse(dateString);
      String yearMonth = yearFormat.format(date) + monthMap.get(monthFormat.format(date));
      LOG.info("yearMonth {} ", yearMonth);
      return yearFormat.format(date) + monthMap.get(monthFormat.format(date));
    } catch (Exception e) {
      LOG.error("Modified Date error date {}", dateString);
    }
    return null;
  }

  private static void print(Exception exception) {
    LOG.error("EXCEPTION: {}", exception);
  }
}
