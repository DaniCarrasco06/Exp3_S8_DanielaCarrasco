package com.duocuc.bankbff.atm.dto;

import java.math.BigDecimal;

public record SaldoDto(Long cuentaOrigenId, BigDecimal saldoDisponible) {}
