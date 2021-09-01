package org.shnux.photooganizer;

import java.util.*;

public class TestClass {
  public static void main(String... sh) {
    //        allPerm(sh[0]);
    System.out.println("allPerm(\"abc\") = " + allPerm("abc"));
  }

  public static Set<String> allPerm(String s) {
    Set<String> perm = new HashSet<>();
    // Handling error scenarios
    if (s == null) {
      return null;
    } else if (s.length() == 0) {
      perm.add("");
      return perm;
    }
    char init = s.charAt(0);
    String rem = s.substring(1);
    Set<String> words = allPerm(rem);
    for (String strNew : words) {
      for (int i = 0; i <= strNew.length(); i++) {
        perm.add(insertChar(strNew, init, i));
      }
    }
    System.out.println("perm = " + perm);
    return perm;
  }

  public static String insertChar(String sh, char init, int i) {
    String begin = sh.substring(0, i);
    String end = sh.substring(i);
    return begin + init + end;
  }

  public static void allPermAgain(String s) {
    if (s == null) {
      return;
    }
    Set<String> st = new HashSet();
    st.add(s);
    for (int i = 0; i < s.length(); i++) {

      for (int j = 0; j < s.length(); j++) {
        char[] tArr = s.toCharArray();
        char init = tArr[i];
        char tmp = tArr[j];
        if (tmp != init) {
          tArr[j] = init;
          tArr[i] = tmp;
          st.add(new String(tArr));
        }
      }
      System.out.println("Perms :: " + st);
    }
    System.out.println("All Perms :: " + st);
  }
}
