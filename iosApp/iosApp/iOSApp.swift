import SwiftUI
import ComposeApp

@main
struct iOSApp: App {
    @UIApplicationDelegateAdaptor(FirebaseAppDelegate.self) private var firebaseAppDelegate

    var body: some Scene {
        WindowGroup {
            ContentView()
                .onOpenURL { url in
                    AuthDeepLinkBridgeKt.handleIncomingAuthRedirect(url: url.absoluteString)
                }
        }
    }
}
