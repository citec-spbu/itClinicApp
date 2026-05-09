import UIKit
import SwiftUI
import ComposeApp

final class PortraitLockedViewController: UIViewController {
    private let childController: UIViewController

    init(childController: UIViewController) {
        self.childController = childController
        super.init(nibName: nil, bundle: nil)
    }

    @available(*, unavailable)
    required init?(coder: NSCoder) {
        fatalError("init(coder:) has not been implemented")
    }

    override func viewDidLoad() {
        super.viewDidLoad()

        addChild(childController)
        childController.view.translatesAutoresizingMaskIntoConstraints = false
        view.addSubview(childController.view)

        NSLayoutConstraint.activate([
            childController.view.topAnchor.constraint(equalTo: view.topAnchor),
            childController.view.bottomAnchor.constraint(equalTo: view.bottomAnchor),
            childController.view.leadingAnchor.constraint(equalTo: view.leadingAnchor),
            childController.view.trailingAnchor.constraint(equalTo: view.trailingAnchor)
        ])

        childController.didMove(toParent: self)
    }

    override var supportedInterfaceOrientations: UIInterfaceOrientationMask {
        .portrait
    }

    override var shouldAutorotate: Bool {
        false
    }

    override var preferredInterfaceOrientationForPresentation: UIInterfaceOrientation {
        .portrait
    }
}

struct ComposeView: UIViewControllerRepresentable {
    let onLaunchReady: () -> Void

    func makeUIViewController(context: Context) -> UIViewController {
        PortraitLockedViewController(
            childController: MainViewControllerKt.MainViewController(onLaunchReady: onLaunchReady)
        )
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

            Text("CiteC")
                .font(.custom("Philosopher-Bold", size: 50))
                .foregroundStyle(Color(red: 0.6235, green: 0.1765, blue: 0.1255))
                .shadow(color: Color.black.opacity(0.18), radius: 10, x: 0, y: 4)
        }
    }
}

struct ContentView: View {
    @State private var isNativeLaunchSplashVisible = true
    @State private var splashStartedAt = Date()
    
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
