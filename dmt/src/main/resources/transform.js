function transformEvent(eventData) {
    try {
        if (eventData.event) {
            eventData.event = eventData.event + "_transform";
        }
        return eventData;
    } catch (e) {
        return eventData;
    }
}
