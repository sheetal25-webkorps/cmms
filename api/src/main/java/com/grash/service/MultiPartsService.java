package com.grash.service;

import com.grash.dto.MultiPartsPatchDTO;
import com.grash.exception.CustomException;
import com.grash.mapper.MultiPartsMapper;
import com.grash.model.MultiParts;
import com.grash.model.Part;
import com.grash.repository.MultiPartsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityManager;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class MultiPartsService {
    private final MultiPartsRepository multiPartsRepository;
    private final CompanyService companyService;
    private final MultiPartsMapper multiPartsMapper;
    private final EntityManager em;
    private final PartService partService;

    @Transactional
    public MultiParts create(MultiParts multiParts, Long companyId) {
        multiParts.setParts(resolveParts(multiParts.getParts(), companyId));
        return create(multiParts);
    }

    @Transactional
    public MultiParts create(MultiParts multiParts) {
        MultiParts savedMultiParts = multiPartsRepository.saveAndFlush(multiParts);
        em.refresh(savedMultiParts);
        return savedMultiParts;
    }

    @Transactional
    public MultiParts update(Long id, MultiPartsPatchDTO multiPartsPatchDTO) {
        if (multiPartsRepository.existsById(id)) {
            MultiParts savedMultiParts = multiPartsRepository.findById(id).get();
            if (multiPartsPatchDTO.getParts() != null) {
                multiPartsPatchDTO.setParts(resolveParts(multiPartsPatchDTO.getParts(),
                        savedMultiParts.getCompany().getId()));
            }
            MultiParts updatedMultiParts =
                    multiPartsRepository.saveAndFlush(multiPartsMapper.updateMultiParts(savedMultiParts,
                            multiPartsPatchDTO));
            em.refresh(updatedMultiParts);
            return updatedMultiParts;
        } else throw new CustomException("Not found", HttpStatus.NOT_FOUND);
    }

    /**
     * Clients reference parts by id only ({"id": 1}). Part is versioned, so such stubs can't be
     * persisted directly; load the managed parts instead and make sure they belong to the company.
     */
    private List<Part> resolveParts(Collection<Part> parts, Long companyId) {
        if (parts == null) return new ArrayList<>();
        List<Part> resolved = new ArrayList<>();
        for (Part part : parts) {
            if (part == null || part.getId() == null)
                throw new CustomException("Invalid part", HttpStatus.NOT_ACCEPTABLE);
            resolved.add(partService.findByIdAndCompany(part.getId(), companyId)
                    .orElseThrow(() -> new CustomException("Part not found", HttpStatus.NOT_FOUND)));
        }
        return resolved;
    }

    public Collection<MultiParts> getAll() {
        return multiPartsRepository.findAll();
    }

    public void delete(Long id) {
        multiPartsRepository.deleteById(id);
    }

    public Optional<MultiParts> findById(Long id) {
        return multiPartsRepository.findById(id);
    }

    public Collection<MultiParts> findByCompany(Long id) {
        return multiPartsRepository.findByCompany_Id(id);
    }

}

