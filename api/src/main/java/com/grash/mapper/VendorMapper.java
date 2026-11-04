package com.grash.mapper;

import com.grash.dto.VendorMiniDTO;
import com.grash.dto.VendorPatchDTO;
import com.grash.dto.VendorPostDTO;
import com.grash.dto.VendorShowDTO;
import com.grash.model.Vendor;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.Mappings;

@Mapper(componentModel = "spring", uses = {CustomFieldValueMapper.class})
public interface VendorMapper {
    // The patch DTO inherits identity, ownership and audit fields; a request body must never overwrite them.
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "company", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    Vendor updateVendor(@MappingTarget Vendor entity, VendorPatchDTO dto);

    @Mappings({})
    VendorPatchDTO toPatchDto(Vendor model);

    VendorMiniDTO toMiniDto(Vendor model);

    VendorShowDTO toShowDto(Vendor model);

    Vendor fromPostDto(VendorPostDTO dto);
}
