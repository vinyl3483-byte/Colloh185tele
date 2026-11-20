import PhotosUI
import SwiftUI

struct TelefamIosProfileSetupView: View {
    @Environment(\.colorScheme) private var colorScheme
    @ObservedObject var sessionStore: TelefamIosSessionStore
    @Binding var themeMode: TelefamThemeMode

    @State private var fullName: String = ""
    @State private var username: String = ""
    @State private var isUsernameAvailable: Bool? = nil
    @State private var selectedCountry: TelefamIosCountryOption = TelefamIosCountryCatalog.defaultOption()
    @State private var phoneDigits: String = ""
    @State private var gender: String = ""
    @State private var dobDate: Date = Calendar.current.date(byAdding: .year, value: -18, to: Date()) ?? Date()
    @State private var dobChosen: Bool = false
    @State private var location: String = ""
    @State private var bio: String = ""

    @State private var photoPickerItem: PhotosPickerItem? = nil
    @State private var rawPickedImage: UIImage? = nil
    @State private var avatarPreview: UIImage? = nil
    @State private var avatarMediaId: String? = nil
    @State private var showCropSheet: Bool = false
    @State private var cropOffset: CGSize = .zero
    @State private var cropScale: CGFloat = 1.0

    @State private var showCountrySheet: Bool = false
    @State private var countrySearch: String = ""
    @State private var showDobPickerSheet: Bool = false
    @State private var localError: String? = nil

    private let genders = ["Female", "Male", "Prefer not to say"]

    var body: some View {
        GeometryReader { geo in
            let isWide = geo.size.width >= 720
            VStack(spacing: 0) {
                headerBar
                ScrollView {
                    if isWide {
                        HStack(alignment: .top, spacing: 28) {
                            VStack(spacing: 18) {
                                avatarPickerSection
                                bioField
                            }
                            .frame(maxWidth: 340)

                            VStack(spacing: 12) {
                                identityFields
                                submitSection
                            }
                            .frame(maxWidth: 460)
                        }
                        .padding(24)
                        .frame(maxWidth: .infinity)
                    } else {
                        VStack(spacing: 14) {
                            avatarPickerSection
                            identityFields
                            bioField
                            submitSection
                        }
                        .padding(.horizontal, 20)
                        .padding(.vertical, 20)
                        .frame(maxWidth: 460)
                        .frame(maxWidth: .infinity)
                    }
                }
                .background(TelefamPalette.background(colorScheme))
                .clipShape(RoundedCornerSheetTop(radius: 24))
            }
            .background(TelefamPalette.crimson.ignoresSafeArea(edges: .top))
        }
        .onAppear {
            if fullName.isEmpty { fullName = sessionStore.fullName }
            if username.isEmpty { username = sessionStore.username }
        }
        .onChange(of: photoPickerItem) { item in
            guard let item else { return }
            Task {
                if let data = try? await item.loadTransferable(type: Data.self),
                   let img = UIImage(data: data) {
                    rawPickedImage = img
                    cropOffset = .zero
                    cropScale = 1.0
                    showCropSheet = true
                }
            }
        }
        .sheet(isPresented: $showCropSheet) { cropDialog }
        .sheet(isPresented: $showCountrySheet) { countryPickerDialog }
        .sheet(isPresented: $showDobPickerSheet) { dobPickerDialog }
    }

    private var headerBar: some View {
        ZStack {
            HStack {
                Button { sessionStore.signOut() } label: {
                    Image(systemName: "chevron.left")
                        .font(.system(size: 18, weight: .bold))
                        .foregroundColor(.white)
                        .frame(width: 40, height: 40)
                }
                Spacer()
                Button { themeMode = themeMode.next } label: {
                    Image(systemName: themeMode == .dark ? "moon.fill" : (themeMode == .light ? "sun.max.fill" : "circle.lefthalf.filled"))
                        .font(.system(size: 15, weight: .semibold))
                        .foregroundColor(.white)
                        .frame(width: 36, height: 36)
                        .background(Color.white.opacity(0.18))
                        .clipShape(Circle())
                }
            }
            VStack(spacing: 2) {
                Text("Complete Your Profile")
                    .font(.system(size: 18, weight: .bold))
                    .foregroundColor(.white)
                Text("Tell people who you are")
                    .font(.system(size: 12))
                    .foregroundColor(Color.white.opacity(0.88))
            }
        }
        .padding(.horizontal, 12)
        .padding(.vertical, 10)
        .background(TelefamPalette.crimson)
    }

    private var avatarPickerSection: some View {
        VStack(spacing: 8) {
            PhotosPicker(selection: $photoPickerItem, matching: .images) {
                ZStack(alignment: .bottomTrailing) {
                    Circle()
                        .stroke(style: StrokeStyle(lineWidth: 1.6, dash: [6, 5]))
                        .foregroundColor(TelefamPalette.crimson)
                        .frame(width: 96, height: 96)

                    Group {
                        if let img = avatarPreview {
                            Image(uiImage: img).resizable().scaledToFill()
                        } else {
                            Image(systemName: "camera.fill")
                                .font(.system(size: 28))
                                .foregroundColor(TelefamPalette.crimson)
                                .frame(maxWidth: .infinity, maxHeight: .infinity)
                                .background(TelefamPalette.blush(colorScheme))
                        }
                    }
                    .frame(width: 86, height: 86)
                    .clipShape(Circle())

                    Image(systemName: "plus")
                        .font(.system(size: 13, weight: .bold))
                        .foregroundColor(.white)
                        .frame(width: 28, height: 28)
                        .background(TelefamPalette.crimson)
                        .clipShape(Circle())
                        .overlay(Circle().stroke(TelefamPalette.background(colorScheme), lineWidth: 2))
                }
            }
            Text("Upload Profile Photo")
                .font(.system(size: 15, weight: .bold))
                .foregroundColor(TelefamPalette.textPrimary(colorScheme))
            Text("Add a photo so people can recognize you")
                .font(.system(size: 12))
                .foregroundColor(TelefamPalette.textMuted(colorScheme))
                .multilineTextAlignment(.center)
        }
    }

    private var identityFields: some View {
        VStack(alignment: .leading, spacing: 12) {
            fieldLabel(icon: "person.fill", title: "Full Name")
            styledInput(placeholder: "Enter full name", text: $fullName)

            fieldLabel(icon: "at", title: "Username")
            HStack {
                TextField("Choose username", text: $username)
                    .textInputAutocapitalization(.never)
                    .disableAutocorrection(true)
                    .onChange(of: username) { newVal in
                        let clean = newVal.lowercased().filter { $0.isLetter || $0.isNumber || $0 == "_" || $0 == "." }
                        if clean != newVal { username = clean }
                        Task {
                            try? await Task.sleep(nanoseconds: 300_000_000)
                            isUsernameAvailable = await sessionStore.checkUsernameAvailability(clean)
                        }
                    }
                if let avail = isUsernameAvailable {
                    Image(systemName: avail ? "checkmark.circle.fill" : "exclamationmark.circle.fill")
                        .foregroundColor(avail ? Color.green : TelefamPalette.crimson)
                }
            }
            .padding(.horizontal, 12)
            .frame(height: 48)
            .background(TelefamPalette.surface(colorScheme))
            .clipShape(RoundedRectangle(cornerRadius: 12))
            .overlay(RoundedRectangle(cornerRadius: 12).stroke(TelefamPalette.border(colorScheme)))

            fieldLabel(icon: "phone.fill", title: "Phone Number")
            HStack(spacing: 0) {
                Button {
                    countrySearch = ""
                    showCountrySheet = true
                } label: {
                    HStack(spacing: 4) {
                        Text(selectedCountry.flag)
                        Text(selectedCountry.dialCode)
                            .font(.system(size: 14, weight: .medium))
                            .foregroundColor(TelefamPalette.textPrimary(colorScheme))
                        Image(systemName: "chevron.down")
                            .font(.system(size: 11))
                            .foregroundColor(TelefamPalette.textMuted(colorScheme))
                    }
                    .padding(.horizontal, 12)
                    .frame(height: 48)
                }
                Rectangle().fill(TelefamPalette.border(colorScheme)).frame(width: 1, height: 26)
                TextField("Phone number", text: $phoneDigits)
                    .keyboardType(.phonePad)
                    .padding(.horizontal, 12)
                    .onChange(of: phoneDigits) { v in
                        let filtered = v.filter(\.isNumber)
                        if filtered != v { phoneDigits = filtered }
                    }
            }
            .frame(height: 48)
            .background(TelefamPalette.surface(colorScheme))
            .clipShape(RoundedRectangle(cornerRadius: 12))
            .overlay(RoundedRectangle(cornerRadius: 12).stroke(TelefamPalette.border(colorScheme)))

            fieldLabel(icon: "person.2", title: "Gender")
            Menu {
                ForEach(genders, id: \.self) { g in
                    Button(g) { gender = g }
                }
            } label: {
                HStack {
                    Text(gender.isEmpty ? "Select gender" : gender)
                        .foregroundColor(gender.isEmpty ? TelefamPalette.textMuted(colorScheme) : TelefamPalette.textPrimary(colorScheme))
                    Spacer()
                    Image(systemName: "chevron.down")
                        .foregroundColor(TelefamPalette.textMuted(colorScheme))
                }
                .padding(.horizontal, 12)
                .frame(height: 48)
                .background(TelefamPalette.surface(colorScheme))
                .clipShape(RoundedRectangle(cornerRadius: 12))
                .overlay(RoundedRectangle(cornerRadius: 12).stroke(TelefamPalette.border(colorScheme)))
            }

            fieldLabel(icon: "calendar", title: "Date of Birth")
            Button { showDobPickerSheet = true } label: {
                HStack {
                    Text(dobChosen ? isoDobString : "Select date of birth")
                        .foregroundColor(dobChosen ? TelefamPalette.textPrimary(colorScheme) : TelefamPalette.textMuted(colorScheme))
                    Spacer()
                    Image(systemName: "calendar")
                        .foregroundColor(TelefamPalette.crimson)
                }
                .padding(.horizontal, 12)
                .frame(height: 48)
                .background(TelefamPalette.surface(colorScheme))
                .clipShape(RoundedRectangle(cornerRadius: 12))
                .overlay(RoundedRectangle(cornerRadius: 12).stroke(TelefamPalette.border(colorScheme)))
            }

            fieldLabel(icon: "mappin.and.ellipse", title: "Location")
            styledInput(placeholder: "City, Country", text: $location)
        }
    }

    private var bioField: some View {
        VStack(alignment: .leading, spacing: 8) {
            fieldLabel(icon: "text.bubble", title: "Bio")
            ZStack(alignment: .bottomTrailing) {
                TextField("Tell people about yourself", text: $bio, axis: .vertical)
                    .padding(12)
                    .padding(.bottom, 18)
                    .frame(minHeight: 100, alignment: .topLeading)
                    .background(TelefamPalette.surface(colorScheme))
                    .clipShape(RoundedRectangle(cornerRadius: 12))
                    .overlay(RoundedRectangle(cornerRadius: 12).stroke(TelefamPalette.border(colorScheme)))
                    .onChange(of: bio) { v in
                        if v.count > 150 { bio = String(v.prefix(150)) }
                    }
                Text("\(bio.count)/150")
                    .font(.system(size: 11.5))
                    .foregroundColor(TelefamPalette.textMuted(colorScheme))
                    .padding(10)
            }
        }
    }

    private var submitSection: some View {
        VStack(spacing: 10) {
            if let err = localError ?? sessionStore.authError {
                Text(err)
                    .font(.system(size: 12.5))
                    .foregroundColor(TelefamPalette.crimson)
                    .multilineTextAlignment(.center)
            }
            Button {
                submitProfile()
            } label: {
                if sessionStore.isBusy {
                    ProgressView().tint(.white)
                } else {
                    Text("Continue")
                        .font(.system(size: 15, weight: .bold))
                }
            }
            .frame(maxWidth: .infinity, minHeight: 50)
            .background(TelefamPalette.crimson)
            .foregroundColor(.white)
            .clipShape(Capsule())
            .disabled(sessionStore.isBusy)
        }
    }

    private var isoDobString: String {
        let f = DateFormatter()
        f.dateFormat = "yyyy-MM-dd"
        f.locale = Locale(identifier: "en_US_POSIX")
        return f.string(from: dobDate)
    }

    private func submitProfile() {
        let cleanName = fullName.trimmingCharacters(in: .whitespacesAndNewlines)
        let cleanUser = username.trimmingCharacters(in: .whitespacesAndNewlines)
        guard cleanName.count >= 2 else { localError = "Enter your full name."; return }
        guard cleanUser.count >= 3, isUsernameAvailable != false else { localError = "Choose an available username."; return }
        guard phoneDigits.count >= 6 else { localError = "Enter a valid phone number."; return }
        guard !gender.isEmpty else { localError = "Select your gender."; return }
        guard dobChosen else { localError = "Select your date of birth."; return }

        let age = Calendar.current.dateComponents([.year], from: dobDate, to: Date()).year ?? 0
        guard age >= 13 else { localError = "You must be at least 13 years old."; return }

        localError = nil
        Task {
            _ = await sessionStore.completeProfileSetup(
                fullName: cleanName,
                username: cleanUser,
                phoneE164: "\(selectedCountry.dialCode)\(phoneDigits)",
                countryIso: selectedCountry.isoCode,
                gender: gender,
                dateOfBirthIso: isoDobString,
                location: location.trimmingCharacters(in: .whitespacesAndNewlines),
                bio: bio.trimmingCharacters(in: .whitespacesAndNewlines),
                avatarMediaId: avatarMediaId
            )
        }
    }

    private var cropDialog: some View {
        NavigationStack {
            VStack(spacing: 18) {
                if let img = rawPickedImage {
                    ZStack {
                        Color.black.opacity(0.88)
                        Image(uiImage: img)
                            .resizable()
                            .scaledToFill()
                            .scaleEffect(cropScale)
                            .offset(cropOffset)
                            .gesture(
                                DragGesture()
                                    .onChanged { val in cropOffset = val.translation }
                            )
                            .gesture(
                                MagnificationGesture()
                                    .onChanged { val in cropScale = max(1.0, min(3.0, val)) }
                            )
                            .frame(width: 240, height: 240)
                            .clipShape(Circle())
                            .overlay(Circle().stroke(Color.white, lineWidth: 2))
                    }
                    .frame(height: 300)
                    .clipShape(RoundedRectangle(cornerRadius: 16))
                }
                Button("Use Photo") {
                    guard let img = rawPickedImage, let data = img.jpegData(compressionQuality: 0.8) else {
                        showCropSheet = false
                        return
                    }
                    avatarPreview = img
                    showCropSheet = false
                    Task {
                        let cx = Float(0.5 - (cropOffset.width / 480.0))
                        let cy = Float(0.5 - (cropOffset.height / 480.0))
                        avatarMediaId = await sessionStore.uploadAvatar(
                            jpegData: data,
                            cx: max(0.0, min(1.0, cx)),
                            cy: max(0.0, min(1.0, cy)),
                            scale: Float(cropScale)
                        )
                    }
                }
                .font(.system(size: 15, weight: .bold))
                .foregroundColor(.white)
                .frame(maxWidth: .infinity, minHeight: 48)
                .background(TelefamPalette.crimson)
                .clipShape(Capsule())
            }
            .padding(20)
            .navigationTitle("Crop Photo")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("Cancel") { showCropSheet = false }
                }
            }
        }
    }

    private var countryPickerDialog: some View {
        NavigationStack {
            VStack {
                TextField("Search country or dial code", text: $countrySearch)
                    .padding(10)
                    .overlay(Capsule().stroke(TelefamPalette.crimson, lineWidth: 1.5))
                    .padding(.horizontal, 16)
                List(TelefamIosCountryCatalog.all.filter {
                    countrySearch.isEmpty ||
                    $0.name.localizedCaseInsensitiveContains(countrySearch) ||
                    $0.dialCode.contains(countrySearch)
                }) { option in
                    Button {
                        selectedCountry = option
                        showCountrySheet = false
                    } label: {
                        HStack {
                            Text(option.flag)
                            Text(option.name).foregroundColor(TelefamPalette.textPrimary(colorScheme))
                            Spacer()
                            Text(option.dialCode).foregroundColor(TelefamPalette.textMuted(colorScheme))
                        }
                    }
                }
            }
            .navigationTitle("Select Country")
            .navigationBarTitleDisplayMode(.inline)
        }
    }

    private var dobPickerDialog: some View {
        NavigationStack {
            VStack {
                DatePicker("Date of Birth", selection: $dobDate, in: ...Date(), displayedComponents: .date)
                    .datePickerStyle(.graphical)
                    .tint(TelefamPalette.crimson)
                Button("OK") {
                    dobChosen = true
                    showDobPickerSheet = false
                }
                .font(.system(size: 15, weight: .bold))
                .foregroundColor(.white)
                .frame(maxWidth: .infinity, minHeight: 48)
                .background(TelefamPalette.crimson)
                .clipShape(Capsule())
                .padding(.horizontal, 16)
            }
            .padding(16)
            .navigationTitle("Date of Birth")
            .navigationBarTitleDisplayMode(.inline)
        }
    }

    private func fieldLabel(icon: String, title: String) -> some View {
        HStack(spacing: 6) {
            Image(systemName: icon)
                .font(.system(size: 13))
                .foregroundColor(TelefamPalette.crimson)
            Text(title)
                .font(.system(size: 13, weight: .semibold))
                .foregroundColor(TelefamPalette.textPrimary(colorScheme))
        }
    }

    private func styledInput(placeholder: String, text: Binding<String>) -> some View {
        TextField(placeholder, text: text)
            .padding(.horizontal, 12)
            .frame(height: 48)
            .background(TelefamPalette.surface(colorScheme))
            .clipShape(RoundedRectangle(cornerRadius: 12))
            .overlay(RoundedRectangle(cornerRadius: 12).stroke(TelefamPalette.border(colorScheme)))
    }
}

private struct RoundedCornerSheetTop: Shape {
    let radius: CGFloat
    func path(in rect: CGRect) -> Path {
        let path = UIBezierPath(
            roundedRect: rect,
            byRoundingCorners: [.topLeft, .topRight],
            cornerRadii: CGSize(width: radius, height: radius)
        )
        return Path(path.cgPath)
    }
}
