package com.optimapc.backend.reciclaje;

import com.optimapc.backend.reciclaje.dto.ReciclajeTipoUsoDto;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

@RestController
@RequestMapping("/api/reciclaje")
public class ReciclajeController {

    private final ReciclajeService reciclajeService;

    public ReciclajeController(ReciclajeService reciclajeService) {
        this.reciclajeService = reciclajeService;
    }

    @PostMapping("/sugerir")
    public ResponseEntity<List<ReciclajeTipoUsoDto>> sugerirConfiguraciones(
            @Valid @RequestBody ReciclajeRequest request) {
        return ResponseEntity.ok(reciclajeService.sugerirConfiguraciones(request.componenteIds()));
    }

    record ReciclajeRequest(@NotEmpty List<Long> componenteIds) {}
}
