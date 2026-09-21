import Foundation

@MainActor
final class ChemLinkStore: ObservableObject {
    @Published var isLoggedIn = false
    @Published var phone = ""
    @Published var listings: [Listing] = []

    private let dbURL = URL(string: "https://chemlink-8909b-default-rtdb.firebaseio.com/chemlink/offers.json")!

    init() {
        isLoggedIn = UserDefaults.standard.bool(forKey: "chemlinkLoggedIn")
        phone = UserDefaults.standard.string(forKey: "chemlinkPhone") ?? ""
        Task { await loadListings() }
    }

    func login(phone: String) {
        self.phone = phone
        isLoggedIn = true
        UserDefaults.standard.set(true, forKey: "chemlinkLoggedIn")
        UserDefaults.standard.set(phone, forKey: "chemlinkPhone")
        Task { await loadListings() }
    }

    func logout() {
        isLoggedIn = false
        UserDefaults.standard.set(false, forKey: "chemlinkLoggedIn")
    }

    func loadListings() async {
        guard let (data, response) = try? await URLSession.shared.data(from: dbURL),
              (response as? HTTPURLResponse)?.statusCode == 200,
              let raw = try? JSONSerialization.jsonObject(with: data) else { return }

        let objects: [[String: Any]]
        if let array = raw as? [[String: Any]] {
            objects = array
        } else if let dict = raw as? [String: Any] {
            objects = dict.values.compactMap { $0 as? [String: Any] }
        } else {
            objects = []
        }

        listings = objects.compactMap { item in
            guard String(describing: item["status"] ?? "").uppercased() == "APPROVED" else { return nil }
            return Listing(
                name: item["name"] as? String ?? "",
                official: item["publishedOfficial"] as? String ?? item["official"] as? String ?? "",
                market: item["publishedMarket"] as? String ?? item["market"] as? String ?? "",
                place: item["place"] as? String ?? "",
                time: item["time"] as? String ?? ""
            )
        }
    }
}

struct Listing: Identifiable {
    let id = UUID()
    let name: String
    let official: String
    let market: String
    let place: String
    let time: String
}
