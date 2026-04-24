package com.br.itau.login.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SessionDTO {
	private String sessionId;
	private String username;
	private Boolean contractService;
	private String symmetricKey;
	private String role;
}
