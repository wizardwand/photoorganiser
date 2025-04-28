package org.shnux.photooganizer;

import com.drew.imaging.ImageMetadataReader;
import com.drew.imaging.ImageProcessingException;
import com.drew.metadata.Directory;
import com.drew.metadata.Metadata;
import com.drew.metadata.Tag;
import com.drew.metadata.exif.ExifSubIFDDirectory;
import com.sampullara.cli.Args;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.tika.parser.AutoDetectParser;
import org.apache.tika.parser.ParseContext;
import org.xml.sax.helpers.DefaultHandler;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Date;
import java.util.List;
import java.util.Objects;

import static org.shnux.photooganizer.DateConverter.formatStringToFolderName;

/**
 * Created by Shirish on 4/17/2021 for learning
 */
public class PhotoOrganizerApplication {

  public static final String PATH_SEPARATOR = File.separator;
  public static final String DONT_KNOW = "dont_know";

  private static final Logger LOG = LogManager.getLogger(PhotoOrganizerApplication.class);
  private static final List<String> ignoreFiles = List.of("aae", "pdf");
  private static long numberOfFiles = 0;
  private static long totalSize = 0;

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
      printAnalysis();
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
        }
      }
    }
  }

  private static void printAnalysis() {

    double kilobytes = (double) totalSize / 1024;
    double megabytes = kilobytes / 1024;
    double gigabytes = megabytes / 1024;
    LOG.error("Total {} Files Processed", numberOfFiles);
    LOG.error("Total size of {} GB Files Processed", gigabytes);
  }

  private static void moveFilesInThisPath(String fileName, String filePath) {
    LOG.info("fileName = {}", filePath);

    try {
      File file = new File(filePath);
      final String yearMonth = extractYearMonth(file);
      LOG.info(
          "Destination = {}",
          Options.destination + PATH_SEPARATOR + yearMonth + PATH_SEPARATOR + fileName);
      moveFileToDirectory(
          filePath, Options.destination + PATH_SEPARATOR + yearMonth + PATH_SEPARATOR + fileName);
    } catch (SkippedException e) {
      LOG.info("skipped");
    } catch (ImageProcessingException | IOException e) {
      LOG.error(" Error Processing file:: {}", fileName);
    } catch (Exception e) {
      LOG.error("Generic Error Processing file:: {}", fileName);
    }
  }

  private static String extractYearMonth(File file) throws Exception {
    String extension = getFileExtension(file);
    if (ignoreFiles.contains(extension)) {
      LOG.info("Skipped");
      throw new SkippedException("Skipped for file extension");
    }

    switch (extension) {
      case "heic":
        return getDateFromExifTool(ImageMetadataReader.readMetadata(file));
      case "mov":
      case "mp4":
        return getMovCreationDate(file);
      case "jpg":
      case "jpeg":
      case "png":
      case "dng":
      case "gif":
      case "avi":
      case "3gp":
        return extractFromMetadata(ImageMetadataReader.readMetadata(file));
      default:
        LOG.warn("Unsupported file type: {}. Using default 'unknown'", extension);
        return DONT_KNOW;
    }
  }

  private static String extractFromMetadata(Metadata metadata) {
    for (Directory directory : metadata.getDirectories()) {
      for (Tag tag : directory.getTags()) {
        if (tag.getTagName().toLowerCase().contains("date")) {
          String date = tag.getDescription(); // e.g., "2023:12:05 15:00:00"
          if (date != null && date.length() >= 7) {
            return formatStringToFolderName(date);
          }
        }
      }
    }
    return DONT_KNOW;
  }

  private static String getDateFromExifTool(Metadata metadata) {
    ExifSubIFDDirectory exifDir = metadata.getFirstDirectoryOfType(ExifSubIFDDirectory.class);
    if (exifDir != null) {
      Date date = exifDir.getDateOriginal();
      if (date != null) {
        String yearMonth = formatStringToFolderName(date.toString());
        LOG.info("Apple date : {} ", yearMonth);
        return yearMonth;
      }
    }
    return DONT_KNOW;
  }

  private static String getMovCreationDate(File file) throws IOException {
    try (FileInputStream input = new FileInputStream(file.getAbsoluteFile())) {
      AutoDetectParser parser = new AutoDetectParser();
      org.apache.tika.metadata.Metadata metadata = new org.apache.tika.metadata.Metadata();
      parser.parse(input, new DefaultHandler(), metadata, new ParseContext());
      String createDate = metadata.get("dcterms:created");
      return createDate != null ? DateConverter.formatStringToFolderName(createDate) : DONT_KNOW;
    } catch (Exception e) {
      return DONT_KNOW;
    }
  }


  private static String getFileExtension(File file) {
    totalSize += file.length();
    String name = file.getName();
    int lastDot = name.lastIndexOf('.');
    return (lastDot == -1) ? "" : name.substring(lastDot + 1).toLowerCase();
  }

  public static void moveFileToDirectory(String fromFile, String toFile) {

    Path source = Paths.get(fromFile);
    Path target = Paths.get(toFile);

    createFolderIfNotPresent(toFile);

    try {

      // rename or move a file to other path
      // if target exists, throws FileAlreadyExistsException
      Files.move(source, target);
      numberOfFiles++;
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
      }
    } catch (Exception e) {
      LOG.error("e = {}", e.getMessage());
    }
  }

}
