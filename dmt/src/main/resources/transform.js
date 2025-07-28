function transformEvent(jsonString) {
    try {
        var eventData = JSON.parse(jsonString);
        if (eventData.event) {
            eventData.event = eventData.event + "_transform";
        }
        return JSON.stringify(eventData);
    } catch (e) {
        return jsonString;
    }
}