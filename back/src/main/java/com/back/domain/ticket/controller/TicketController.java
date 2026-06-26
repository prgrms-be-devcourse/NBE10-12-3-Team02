package com.back.domain.ticket.controller;

import com.back.global.annotation.ApiV1;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@ApiV1
@RestController
@RequestMapping
@RequiredArgsConstructor
@Tag(name = "Ticket", description = "Ticket API")
public class TicketController {
}
