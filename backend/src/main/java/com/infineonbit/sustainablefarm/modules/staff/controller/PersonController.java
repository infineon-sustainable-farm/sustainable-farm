package com.infineonbit.sustainablefarm.modules.staff.controller;

import com.infineonbit.sustainablefarm.modules.staff.api.PersonDirectory;
import com.infineonbit.sustainablefarm.modules.staff.api.PersonDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Read-only list of the people directory, for the modules that pick a person (operator,
 * technician, observer...). People are created, edited and deactivated only by the visitor
 * management Staff screen ({@code /api/visitor-management/staff}).
 */
@RestController
@RequestMapping("/api/staff/persons")
@Tag(name = "People", description = "People directory, read only")
public class PersonController {

    private final PersonDirectory personDirectory;

    public PersonController(PersonDirectory personDirectory) {
        this.personDirectory = personDirectory;
    }

    @GetMapping
    @Operation(summary = "List the active people, optionally those whose name or email contains search")
    public List<PersonDto> list(@RequestParam(required = false) String search) {
        return personDirectory.searchActive(search);
    }
}
