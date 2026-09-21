import SwiftUI

struct RootView: View {
    @EnvironmentObject private var store: ChemLinkStore
    @State private var showLogin = false

    var body: some View {
        NavigationStack {
            Group {
                if store.isLoggedIn {
                    MarketView(showLogin: $showLogin)
                } else {
                    LockedView { showLogin = true }
                }
            }
            .navigationTitle("ChemLink")
            .toolbar {
                if store.isLoggedIn {
                    Button("حساب") { showLogin = true }
                }
            }
            .sheet(isPresented: $showLogin) {
                LoginView()
            }
        }
    }
}

struct LockedView: View {
    let action: () -> Void
    var body: some View {
        VStack(spacing: 18) {
            Image(systemName: "flask.fill").font(.system(size: 54))
            Text("بازار تخصصی مواد شیمیایی").font(.title2.bold())
            Text("برای دیدن آگهی‌ها و ثبت آگهی وارد حساب شوید.")
                .multilineTextAlignment(.center)
                .foregroundStyle(.secondary)
            Button("ورود با شماره موبایل", action: action)
                .buttonStyle(.borderedProminent)
        }
        .padding(28)
    }
}

struct MarketView: View {
    @EnvironmentObject private var store: ChemLinkStore
    @Binding var showLogin: Bool
    @State private var query = ""

    var filtered: [Listing] {
        guard !query.isEmpty else { return store.listings }
        return store.listings.filter {
            "\($0.name) \($0.official) \($0.market) \($0.place)".localizedCaseInsensitiveContains(query)
        }
    }

    var body: some View {
        VStack {
            HStack {
                TextField("جستجوی مواد شیمیایی", text: $query)
                    .textFieldStyle(.roundedBorder)
                Button("ثبت آگهی") { showLogin = true }
                    .buttonStyle(.borderedProminent)
            }
            .padding(.horizontal)

            if filtered.isEmpty {
                ContentUnavailableView("آگهی تأییدشده‌ای وجود ندارد", systemImage: "shippingbox")
            } else {
                List(filtered) { item in
                    VStack(alignment: .leading, spacing: 6) {
                        Text(item.name).font(.headline)
                        if !item.official.isEmpty { Text(item.official) }
                        if !item.market.isEmpty { Text(item.market) }
                        Text([item.place, item.time].filter { !$0.isEmpty }.joined(separator: " • "))
                            .font(.caption)
                            .foregroundStyle(.secondary)
                        Button("مشاهده جزئیات و خرید") {
                            // Contact flow will be connected to manager WhatsApp/Telegram.
                        }
                        .buttonStyle(.bordered)
                    }
                    .padding(.vertical, 5)
                }
                .listStyle(.plain)
            }
        }
    }
}

struct LoginView: View {
    @EnvironmentObject private var store: ChemLinkStore
    @Environment(\.dismiss) private var dismiss
    @State private var phone = ""

    var body: some View {
        NavigationStack {
            Form {
                Section("ورود به ChemLink") {
                    TextField("شماره موبایل", text: $phone)
                        .keyboardType(.phonePad)
                    Text("ورود با پیامک در نسخه نهایی به سرویس SMS متصل می‌شود.")
                        .font(.footnote)
                        .foregroundStyle(.secondary)
                }
                Button("ورود") {
                    store.login(phone: phone)
                    dismiss()
                }
                .disabled(phone.trimmingCharacters(in: .whitespaces).isEmpty)
            }
            .navigationTitle("ورود")
        }
    }
}
