import SwiftUI

@main
struct ChemLinkApp: App {
    @StateObject private var store = ChemLinkStore()

    var body: some Scene {
        WindowGroup {
            RootView()
                .environmentObject(store)
                .environment(\.layoutDirection, .rightToLeft)
        }
    }
}
