package com.orenkohavi.sidechatproxy

//PostType enum with Hot, New, and Top
enum class PostType {
    Hot,
    New,
    Top
}

//Lookup function to convert from PostType to String
fun PostType.as_string(): String {
    if (this == PostType.Hot) {
        return "hot"
    } else if (this == PostType.New) {
        return "recent"
    } else if (this == PostType.Top) {
        return "top"
    } else {
        throw IllegalStateException("Invalid PostType: $this")
    }
}

//Lookup function to convert from String to PostType
fun String.getPostType(): PostType {
    return when (this) {
        "hot" -> PostType.Hot
        "recent" -> PostType.New
        "top" -> PostType.Top
        else -> throw IllegalStateException("Invalid PostType string: $this")
    }
}