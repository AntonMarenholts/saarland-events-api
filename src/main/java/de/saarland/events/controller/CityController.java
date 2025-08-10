

package de.saarland.events.controller;

import de.saarland.events.dto.CityDto;
import de.saarland.events.mapper.CityMapper;
import de.saarland.events.service.CityService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/cities")
public class CityController {

    private final CityService cityService;
    private final CityMapper cityMapper;

    public CityController(CityService cityService, CityMapper cityMapper) {
        this.cityService = cityService;
        this.cityMapper = cityMapper;
    }

    @GetMapping
    public ResponseEntity<List<CityDto>> getAllCities() {
        // Получаем все города из сервиса
        List<CityDto> cities = cityService.getAllCities().stream()
                .map(cityMapper::toDto)
                .collect(Collectors.toList());

        return ResponseEntity.ok(cities);
    }
}