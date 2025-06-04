package com.eventitta.event.service;

import lombok.Getter;

import java.util.List;

@Getter
public record PageResult<T>(List<T> items, int totalCount) {

}
