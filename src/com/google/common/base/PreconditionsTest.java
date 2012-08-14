package com.google.common.base;

import javax.annotation.Nullable;

public class PreconditionsTest
{

	/**
	 * @param args
	 */
	public static void main(String[] args)
	{
		String template = "code %s, %s the rest";
		String[] errorMessages = {"one", "two", "three"};
		System.out.println(formatTest(template, errorMessages));

	}
	
	private static String formatTest(String template, @Nullable Object... args)
	{
		template = "code %s, %s the rest";
		String[] errorMessages = {"one", "two", "three"};
		return Preconditions.format(template, errorMessages);
	}

}
