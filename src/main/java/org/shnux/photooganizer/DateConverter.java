package org.shnux.photooganizer;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class DateConverter {

  public static final String DONT_KNOW = "dont_know";

  protected static final Map<String, String> monthMap = Stream.of(
    new String[][] {
      {"Jan", "_01_Jan"}, {"Feb", "_02_Feb"}, {"Mar", "_03_Mar"},
      {"Apr", "_04_Apr"}, {"May", "_05_May"}, {"Jun", "_06_Jun"},
      {"Jul", "_07_Jul"}, {"Aug", "_08_Aug"}, {"Sep", "_09_Sep"},
      {"Oct", "_10_Oct"}, {"Nov", "_11_Nov"}, {"Dec", "_12_Dec"}
    }).collect(Collectors.toMap(data -> data[0], data -> data[1]));

  protected static final Map<String, String> intMonthMap = Stream.of(
    new String[][] {
      {"01", "_01_Jan"}, {"02", "_02_Feb"}, {"03", "_03_Mar"},
      {"04", "_04_Apr"}, {"05", "_05_May"}, {"06", "_06_Jun"},
      {"07", "_07_Jul"}, {"08", "_08_Aug"}, {"09", "_09_Sep"},
      {"10", "_10_Oct"}, {"11", "_11_Nov"}, {"12", "_12_Dec"}
    }).collect(Collectors.toMap(data -> data[0], data -> data[1]));

  private static final List<SimpleDateFormat> supportedFormats = Arrays.asList(
    new SimpleDateFormat("yyyy:MM:dd HH:mm:ss", Locale.ROOT),
    new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.ROOT),
    new SimpleDateFormat("yyyy/MM/dd HH:mm:ss", Locale.ROOT),
    new SimpleDateFormat("EEE MMM dd HH:mm:ss zzz yyyy", Locale.ENGLISH),
    new SimpleDateFormat("EEE MMM dd HH:mm:ss XXX yyyy", Locale.ENGLISH),
      new SimpleDateFormat("EEE MMM dd HH:mm:ss z yyyy", Locale.ENGLISH),
      new SimpleDateFormat("EEE MMM dd HH:mm:ss yyyy", Locale.ENGLISH),
    new SimpleDateFormat("yyyy-MM-dd", Locale.ROOT),
    new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssX"),
    new SimpleDateFormat("yyyy:MM:dd", Locale.ROOT)
  );

  public static String formatStringToFolderName(String dateString) {
    for (DateFormat format : supportedFormats) {
      try {
        Date date = format.parse(dateString);
        return formatToMonthFolder(date);
      } catch (ParseException ignored) {
        // Try the next format
      }
    }
    return DONT_KNOW;
  }

  public static String formatToMonthFolder(Date date) {
    SimpleDateFormat yearFormat = new SimpleDateFormat("yyyy");
    SimpleDateFormat monthFormat = new SimpleDateFormat("MM");

    String year = yearFormat.format(date);
    String month = monthFormat.format(date);

    return year + intMonthMap.getOrDefault(month, "_" + month);
  }
}
