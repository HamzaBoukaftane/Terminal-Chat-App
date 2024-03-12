/*********************************************************************
 *
 * This file java file contains the class InputValidator which
 * contains two static methods to validate IP address format and port
 * number.
 *
 * file: InputValidator.java
 * authors: Hamza Boukaftane, Mehdi El Harami and Omar Benzekri
 * date: 18 may 2023
 * modified: 21 may 2023
 *
 **********************************************************************/

import java.util.regex.Pattern;
import java.util.regex.Matcher;

public class InputValidator {
	
	private static final Pattern pattern = Pattern.compile(
			"^([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\." +
	        "([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\."  +
	        "([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\."  +
	        "([01]?\\d\\d?|2[0-4]\\d|25[0-5])$"
		);

	/**
	*
	* This method verifies if the given user input is a valid IP address.
	* 
	* @param String userInput the user input to validate
	* @return boolean true if the user input is a valid IP address, false otherwise
	* 
	*/
	public static boolean isValidIPAddress(String UserInput) {
		Matcher matcher = pattern.matcher(UserInput);
		return matcher.matches();
	}
	
	/**
	*
	* This method verifies if the given user input is a valid port number.
	* 
	* @param String userInput the user input to validate
	* @return boolean true if the user input is a valid port number, false otherwise
	* 
	*/
	public static boolean isValidPortNumber(int userInput) {
		return 5000 <= userInput && userInput <= 5050;
	}
}
