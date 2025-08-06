function transformEvent(jsonString) {
    try {
        var eventData = JSON.parse(jsonString);
        if (eventData.event) {
            eventData.event = eventData.event + "_transform";
        }
        return eventData;
    } catch (e) {
        return jsonString;
    }
}
