function transformEvent(eventData) {
    if (eventData.event) {
        eventData.event = eventData.event + "_transform";
    }
    return eventData;
}
