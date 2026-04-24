package com.br.itau.login.exception;

public class UserNotFoundException extends RuntimeException {

	private static final long serialVersionUID = 3370583014886620246L;

	public UserNotFoundException(String message) {
		super(message);
	}

}
