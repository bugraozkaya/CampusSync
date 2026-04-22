from django.db import migrations


class Migration(migrations.Migration):

    dependencies = [
        ('scheduler', '0005_chatmessage_coursematerial_grade'),
    ]

    operations = [
        migrations.RemoveField(
            model_name='user',
            name='plain_password',
        ),
    ]
