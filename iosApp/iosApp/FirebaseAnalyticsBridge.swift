import Foundation
import FirebaseAnalytics
import ComposeApp

final class FirebaseAnalyticsSinkImpl: NSObject, NativeFirebaseAnalyticsSink {
    func logEvent(name: String, payloadJson: String) {
        let parameters = decodeParameters(from: payloadJson)
        Analytics.logEvent(name, parameters: parameters.isEmpty ? nil : parameters)
    }

    func setUserId(userId: String?) {
        Analytics.setUserID(userId)
    }

    func setUserProperty(name: String, value: String?) {
        Analytics.setUserProperty(value, forName: name)
    }

    func resetAnalyticsData() {
        Analytics.resetAnalyticsData()
    }

    private func decodeParameters(from payloadJson: String) -> [String: Any] {
        guard
            let data = payloadJson.data(using: .utf8),
            let raw = try? JSONSerialization.jsonObject(with: data) as? [String: Any]
        else {
            return [:]
        }

        var parameters: [String: Any] = [:]

        if let strings = raw["strings"] as? [String: String] {
            parameters.merge(strings) { _, new in new }
        }

        if let longs = raw["longs"] as? [String: NSNumber] {
            parameters.merge(longs) { _, new in new }
        }

        if let doubles = raw["doubles"] as? [String: NSNumber] {
            parameters.merge(doubles) { _, new in new }
        }

        return parameters
    }
}
