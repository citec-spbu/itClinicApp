import UIKit
import SwiftUI
import ComposeApp

struct ComposeView: UIViewControllerRepresentable {
    let onLaunchReady: () -> Void

    func makeUIViewController(context: Context) -> UIViewController {
        let controller = MainViewControllerKt.MainViewController(onLaunchReady: onLaunchReady)
        
        // Настройка статус-бара: светлый фон с темным содержимым
        controller.overrideUserInterfaceStyle = .light
        
        return controller
    }

    func updateUIViewController(_ uiViewController: UIViewController, context: Context) {}
}

private struct NativeLaunchSplashView: View {
    var body: some View {
        ZStack {
            Color.white
                .ignoresSafeArea()

            Image("LaunchSpbuLogo")
                .resizable()
                .aspectRatio(contentMode: .fit)
                .frame(maxWidth: .infinity, maxHeight: .infinity)

            Text("Citec")
                .font(.custom("Philosopher-Bold", size: 50))
                .foregroundStyle(Color(red: 0.6235, green: 0.1765, blue: 0.1255))
                .shadow(color: Color.black.opacity(0.18), radius: 10, x: 0, y: 4)
        }
    }
}

struct ContentView: View {
    @State private var isNativeLaunchSplashVisible = true
    @State private var splashStartedAt = Date()

    init() {
        // Настройка внешнего вида статус-бара
        UIApplication.shared.statusBarStyle = .darkContent
    }
    
    var body: some View {
        ZStack {
            ComposeView {
                let remainingDelay = max(0, 2 - Date().timeIntervalSince(splashStartedAt))
                DispatchQueue.main.asyncAfter(deadline: .now() + remainingDelay) {
                    withAnimation(.easeOut(duration: 0.35)) {
                        isNativeLaunchSplashVisible = false
                    }
                }
            }
            .ignoresSafeArea()
            .preferredColorScheme(.light)

            if isNativeLaunchSplashVisible {
                NativeLaunchSplashView()
                    .transition(
                        .asymmetric(
                            insertion: .identity,
                            removal: .opacity.combined(with: .scale(scale: 1.015))
                        )
                    )
            }
        }
    }
}
