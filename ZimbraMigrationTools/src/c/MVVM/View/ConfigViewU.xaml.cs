﻿using MVVM.ViewModel;
using System.Windows;

namespace MVVM.View
{
    public partial class ConfigViewU
    {
        public ConfigViewU()
        {
            InitializeComponent();
        }

        // Kind of a drag that we have to put these next 2 methods in here, but PasswordBox is not a dependency property,
        // so we can't bind to the model.  Doing this for now -- should probably use an attached property later
        private ConfigViewModelU ViewModel
        {
            get { return DataContext as ConfigViewModelU; }
        }

        private void pb_PasswordChanged(object sender, RoutedEventArgs e)
        {
            ViewModel.ZimbraUserPasswd = MVVM.Model.PasswdCrypt.Encrypt(passwordBox1.Password,"ZIMBRA");
        }
    }

}
