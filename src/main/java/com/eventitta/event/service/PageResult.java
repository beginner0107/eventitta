package com.eventitta.event.service;

import java.util.List;

public record PageResult<T>(List<T> items, int totalCount) {

}
