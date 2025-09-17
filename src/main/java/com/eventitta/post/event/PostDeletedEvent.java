package com.eventitta.post.event;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
public class PostDeletedEvent {
    private final List<String> imageUrls;
}

