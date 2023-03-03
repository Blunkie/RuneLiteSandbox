package com.gestankbratwurst.utils;

public class ArrayUtils {

  public static <T> boolean contains(T[] array, T element) {
    for(T t : array) {
      if(element.equals(t)) {
        return true;
      }
    }
    return false;
  }

  public static boolean contains(int[] array, int element) {
    for(int i : array) {
      if(i == element) {
        return true;
      }
    }
    return false;
  }

}
