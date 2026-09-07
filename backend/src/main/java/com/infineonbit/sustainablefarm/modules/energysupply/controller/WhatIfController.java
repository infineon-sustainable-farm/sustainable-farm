package com.infineonbit.sustainablefarm.modules.energysupply.controller;

import com.infineonbit.sustainablefarm.modules.energysupply.dto.WhatIfRequestDto;
import com.infineonbit.sustainablefarm.modules.energysupply.dto.WhatIfResponseDto;
import com.infineonbit.sustainablefarm.modules.energysupply.service.WhatIfService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Feeds the "What-If Scenario Analysis" tab of the Energy App. */
@RestController
@RequestMapping("/api/energy/whatif")
public class WhatIfController {

    private final WhatIfService whatIfService;

    @Autowired
    public WhatIfController(WhatIfService whatIfService) {
        this.whatIfService = whatIfService;
    }

    @PostMapping("/simulate")
    public WhatIfResponseDto simulate(@RequestBody WhatIfRequestDto request) {
        return whatIfService.simulate(request);
    }
}
