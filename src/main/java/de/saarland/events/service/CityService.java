package de.saarland.events.service;

import de.saarland.events.model.City;
import de.saarland.events.repository.CityRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class CityService {

    private final CityRepository cityRepository;

    public CityService(CityRepository cityRepository) {
        this.cityRepository = cityRepository;
    }

    @Transactional(readOnly = true)
    public List<City> getAllCities() {
        return cityRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Optional<City> getCityById(Long id) {
        return cityRepository.findById(id);
    }

    @Transactional
    public City saveCity(City city) {

        return cityRepository.save(city);
    }

    @Transactional
    public void deleteCity(Long id) {
        if (!cityRepository.existsById(id)) {
            throw new EntityNotFoundException("Cannot delete. City with ID " + id + " not found.");
        }
        cityRepository.deleteById(id);
    }

    @Transactional
    public City updateCity(Long id, City cityDetails) {
        City existingCity = cityRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("City with ID " + id + " not found"));


        Optional<City> cityWithSameName = cityRepository.findByName(cityDetails.getName());
        if (cityWithSameName.isPresent() && !cityWithSameName.get().getId().equals(id)) {
            throw new IllegalArgumentException("City with a name '" + cityDetails.getName() + "' already exists.");
        }

        existingCity.setName(cityDetails.getName());
        return cityRepository.save(existingCity);
    }
}