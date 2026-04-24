package com.br.itau.login.model.request;

import java.io.Serializable;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class LoginRequest implements Serializable {
	private static final long serialVersionUID = 7371291438364237383L;
	private String username;
	private String password;
}
