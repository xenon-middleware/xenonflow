package org.commonwl.cwl.utils;

import java.util.Iterator;

public class IteratorUtils
{
	public static<T> Iterable<T> iterable(Iterator<T> iterator) {
		return new Iterable<T>() {
			@Override
			public Iterator<T> iterator() {
				return iterator;
			}
		};
	}
}