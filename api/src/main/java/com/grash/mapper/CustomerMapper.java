package com.grash.mapper;

import com.grash.dto.CustomerMiniDTO;
import com.grash.dto.CustomerPatchDTO;
import com.grash.dto.CustomerPostDTO;
import com.grash.dto.CustomerShowDTO;
import com.grash.model.Customer;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.Mappings;

@Mapper(componentModel = "spring", uses = {CustomFieldValueMapper.class})
public interface CustomerMapper {
    // The patch DTO inherits identity, ownership and audit fields; a request body must never overwrite them.
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "company", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    Customer updateCustomer(@MappingTarget Customer entity, CustomerPatchDTO dto);

    @Mappings({})
    CustomerPatchDTO toPatchDto(Customer model);

    CustomerMiniDTO toMiniDto(Customer model);

    CustomerShowDTO toShowDto(Customer model);

    Customer fromPostDto(CustomerPostDTO dto);
}
