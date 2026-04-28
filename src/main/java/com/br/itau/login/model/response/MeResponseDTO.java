package com.br.itau.login.model.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class MeResponseDTO {
    private String sessionId;
    private String username;
    private Boolean contractService;
    private String role;
    private Long id;
    private String channel;
    
}
