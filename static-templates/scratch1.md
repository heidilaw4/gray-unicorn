.navbar-brand

        // Logo
        a.navbar-item(href='/')
          img.logo.is-hidden-tablet(src='assets/images/logo-compact.svg' width='30' height='30')
          img.logo.is-hidden-mobile(src='assets/images/logo.svg' width='120' height='35')

        // Hamburger menu
        label.navbar-burger.burger(for='headerNavToggle')
          span(aria-hidden='true')
          span(aria-hidden='true')
          span(aria-hidden='true')

      // Hidden checkbox to toggle mobile menu
      input#headerNavToggle.is-hidden.nav-toggle(type='checkbox')