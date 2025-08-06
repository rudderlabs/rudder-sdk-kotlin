function transformEvent(event) {
    if (event.event) {
        event.event = event.event + "_transform";
    }
    return event;
}
